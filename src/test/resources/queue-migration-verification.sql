-- 전용 테스트 PostgreSQL에서 psql -v ON_ERROR_STOP=1로 실행한다.
BEGIN;
CREATE SCHEMA queue_migration_verification;
SET LOCAL search_path TO queue_migration_verification;
CREATE TABLE festivals(festival_id BIGINT PRIMARY KEY);
CREATE TABLE admin_accounts(id BIGINT PRIMARY KEY);
CREATE TABLE field_staff_accounts(id BIGINT PRIMARY KEY);
CREATE TABLE booth_info(booth_id BIGINT PRIMARY KEY, festival_id BIGINT);
CREATE TYPE congestion_level AS ENUM ('LOW','MEDIUM','HIGH');
CREATE TABLE booth_congestion(congestion_id BIGINT PRIMARY KEY, booth_id BIGINT,
    wait_minutes INTEGER, congestion_level congestion_level, updated_at TIMESTAMP);
CREATE TABLE booth_queue(queue_id BIGSERIAL PRIMARY KEY, public_id UUID NOT NULL UNIQUE,
    festival_id BIGINT, booth_id BIGINT UNIQUE, tail_latitude NUMERIC(10,7),tail_longitude NUMERIC(10,7),
    queue_tail_meters INTEGER, path_geometry JSONB,modifier_type VARCHAR(20),modifier_admin_id BIGINT,
    modifier_staff_id BIGINT,created_at TIMESTAMP DEFAULT NOW(),updated_at TIMESTAMP DEFAULT NOW());
INSERT INTO festivals VALUES(1);
INSERT INTO booth_info VALUES(1,1),(2,1);
INSERT INTO booth_queue(public_id,festival_id,booth_id,queue_tail_meters,path_geometry)
    VALUES(gen_random_uuid(),1,1,20,'[{"lat":37,"lng":127},{"lat":37.00018,"lng":127}]');
INSERT INTO booth_congestion VALUES(1,1,5,'LOW','2026-09-16 09:00:00'),(2,1,10,'LOW','2026-09-16 10:00:00'),
    (3,2,30,'MEDIUM','2026-09-16 11:00:00');
\ir ../../main/resources/db/migration/V29__booth_queue_plans_and_observations.sql
DO $$ BEGIN
    IF (SELECT COUNT(*) FROM booth_queue) <> 2 THEN RAISE EXCEPTION 'missing queue backfill'; END IF;
    IF (SELECT wait_minutes FROM booth_queue WHERE booth_id=1) <> 10 THEN RAISE EXCEPTION 'wait not preserved'; END IF;
    IF (SELECT wait_minutes FROM booth_queue WHERE booth_id=2) IS DISTINCT FROM 30 THEN
        RAISE EXCEPTION 'backfilled queue history lost'; END IF;
    IF (SELECT observed_at FROM booth_queue WHERE booth_id=1) <> '2026-09-16 10:00:00'::timestamp THEN
        RAISE EXCEPTION 'observation time not preserved'; END IF;
    IF (SELECT COUNT(*) FROM booth_queue_plan) <> 0 THEN RAISE EXCEPTION 'legacy path converted to plan'; END IF;
    IF (SELECT path_geometry IS NULL FROM booth_queue WHERE booth_id=1) THEN RAISE EXCEPTION 'legacy path lost'; END IF;
END $$;
ROLLBACK;
