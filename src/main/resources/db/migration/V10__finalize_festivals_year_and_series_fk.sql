DO $$
DECLARE
    null_year_count BIGINT;
BEGIN
    IF to_regclass('festivals') IS NULL THEN
        RETURN;
    END IF;

    ALTER TABLE festivals
        ADD COLUMN IF NOT EXISTS festival_year INTEGER;

    UPDATE festivals
    SET festival_year = EXTRACT(YEAR FROM start_date)::INTEGER
    WHERE festival_year IS NULL
      AND start_date IS NOT NULL;

    UPDATE festivals
    SET festival_year = EXTRACT(YEAR FROM loaded_at)::INTEGER
    WHERE festival_year IS NULL
      AND loaded_at IS NOT NULL;

    -- start_date·loaded_at 모두 없는 레거시 행도 기동을 막지 않도록 최후 보정
    UPDATE festivals
    SET festival_year = EXTRACT(YEAR FROM CLOCK_TIMESTAMP())::INTEGER
    WHERE festival_year IS NULL;

    SELECT COUNT(*)
    INTO null_year_count
    FROM festivals
    WHERE festival_year IS NULL;

    IF null_year_count > 0 THEN
        RAISE EXCEPTION
            'festivals.festival_year still NULL for % row(s) after fallback',
            null_year_count;
    END IF;

    ALTER TABLE festivals
        ALTER COLUMN festival_year SET NOT NULL;

    IF to_regclass('festival_series') IS NOT NULL
       AND NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_festivals_series'
       )
       AND NOT EXISTS (
            SELECT 1
            FROM festivals f
            WHERE f.series_id IS NOT NULL
              AND NOT EXISTS (
                    SELECT 1
                    FROM festival_series s
                    WHERE s.series_id = f.series_id
              )
       ) THEN
        ALTER TABLE festivals
            ADD CONSTRAINT fk_festivals_series
            FOREIGN KEY (series_id)
            REFERENCES festival_series (series_id);
    END IF;
END
$$;
