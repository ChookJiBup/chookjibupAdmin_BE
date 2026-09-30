-- 사용자 서버와 기존 파이프라인이 읽고 쓰는 소문자 enum 계약을 유지한다.
-- 관리자 DB처럼 진행 상태 컬럼이 아직 없는 환경도 같은 스키마로 보완한다.
DO $$
BEGIN
    IF to_regtype('festival_progress_status') IS NULL THEN
        CREATE TYPE festival_progress_status AS ENUM ('upcoming', 'ongoing', 'completed');
    END IF;
END
$$;

ALTER TABLE festivals
    ADD COLUMN IF NOT EXISTS progress_status festival_progress_status,
    ADD COLUMN IF NOT EXISTS progress_status_updated_at timestamptz,
    ADD COLUMN IF NOT EXISTS progress_status_override varchar(20);

-- 과거 환경에 문자열 컬럼이 있으면 기존 값을 보존하면서 공용 enum으로 맞춘다.
DO $$
DECLARE
    progress_type text;
BEGIN
    SELECT udt_name INTO progress_type
    FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = 'festivals'
      AND column_name = 'progress_status';

    IF progress_type IS DISTINCT FROM 'festival_progress_status' THEN
        ALTER TABLE festivals
            ALTER COLUMN progress_status TYPE festival_progress_status
            USING lower(progress_status::text)::festival_progress_status;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'festivals'::regclass
          AND conname = 'ck_festivals_progress_status_override'
    ) THEN
        ALTER TABLE festivals ADD CONSTRAINT ck_festivals_progress_status_override
            CHECK (progress_status_override IN ('UPCOMING', 'ONGOING', 'COMPLETED'));
    END IF;
END
$$;

CREATE FUNCTION festival_effective_progress(start_on date, end_on date, override_status text)
RETURNS festival_progress_status LANGUAGE sql STABLE AS $$
    SELECT CASE
        WHEN override_status IS NOT NULL THEN lower(override_status)
        WHEN start_on IS NULL OR end_on IS NULL THEN NULL
        WHEN (statement_timestamp() AT TIME ZONE 'Asia/Seoul')::date < start_on THEN 'upcoming'
        WHEN (statement_timestamp() AT TIME ZONE 'Asia/Seoul')::date > end_on THEN 'completed'
        ELSE 'ongoing'
    END::festival_progress_status
$$;

CREATE FUNCTION synchronize_festival_progress() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.progress_status := festival_effective_progress(
        NEW.start_date, NEW.end_date, NEW.progress_status_override);
    IF TG_OP = 'INSERT' THEN
        NEW.progress_status_updated_at := statement_timestamp();
    ELSIF NEW.progress_status IS DISTINCT FROM OLD.progress_status
       OR NEW.progress_status_override IS DISTINCT FROM OLD.progress_status_override THEN
        NEW.progress_status_updated_at := statement_timestamp();
    ELSE
        NEW.progress_status_updated_at := OLD.progress_status_updated_at;
    END IF;
    RETURN NEW;
END;
$$;

-- 기존 파이프라인의 UPDATE/UPSERT도 수동 지정값을 덮어쓰지 못한다.
CREATE TRIGGER trg_festivals_progress
BEFORE INSERT OR UPDATE OF start_date, end_date, progress_status, progress_status_override
ON festivals FOR EACH ROW EXECUTE FUNCTION synchronize_festival_progress();

UPDATE festivals SET progress_status = progress_status;
