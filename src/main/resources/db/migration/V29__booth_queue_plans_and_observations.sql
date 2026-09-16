CREATE TABLE booth_queue_plan (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    festival_id BIGINT NOT NULL REFERENCES festivals(festival_id) ON DELETE CASCADE,
    booth_id BIGINT NOT NULL UNIQUE REFERENCES booth_info(booth_id) ON DELETE CASCADE,
    path_geometry JSONB NOT NULL,
    length_meters DOUBLE PRECISION NOT NULL CHECK (length_meters > 0 AND length_meters <= 5000),
    meters_per_person DOUBLE PRECISION NOT NULL CHECK (meters_per_person BETWEEN 0.2 AND 5),
    served_persons_per_minute DOUBLE PRECISION NOT NULL CHECK (served_persons_per_minute BETWEEN 0.1 AND 100),
    source_node_id UUID,
    modifier_admin_id BIGINT REFERENCES admin_accounts(id) ON DELETE SET NULL,
    revision BIGINT NOT NULL CHECK (revision > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CLOCK_TIMESTAMP(),
    updated_at TIMESTAMP NOT NULL DEFAULT CLOCK_TIMESTAMP()
);

ALTER TABLE booth_queue ADD COLUMN wait_minutes INTEGER CHECK (wait_minutes >= 0);
ALTER TABLE booth_queue ADD COLUMN congestion_level VARCHAR(20) CHECK (congestion_level IN ('LOW', 'MEDIUM', 'HIGH'));
ALTER TABLE booth_queue ADD COLUMN observed_at TIMESTAMP;
ALTER TABLE booth_queue ADD COLUMN observation_revision BIGINT NOT NULL DEFAULT 0 CHECK (observation_revision >= 0);
ALTER TABLE booth_queue ADD COLUMN plan_revision BIGINT CHECK (plan_revision > 0);
ALTER TABLE booth_queue ADD COLUMN calculation_method VARCHAR(30);

-- 누락 큐를 먼저 생성해야 혼잡 이력만 있는 승인 부스도 현재 대기시간을 복원할 수 있다.
INSERT INTO booth_queue(public_id, festival_id, booth_id)
SELECT gen_random_uuid(), b.festival_id, b.booth_id FROM booth_info b
WHERE NOT EXISTS (SELECT 1 FROM booth_queue q WHERE q.booth_id = b.booth_id)
ON CONFLICT (booth_id) DO NOTHING;

-- 기존 대기시간과 관측 시각은 보존한다. 기존 경로를 사전 계획으로 자동 변환하지 않는다.
UPDATE booth_queue q SET wait_minutes = c.wait_minutes,
    congestion_level = c.congestion_level::text, observed_at = c.updated_at,
    calculation_method = 'LEGACY'
FROM (SELECT DISTINCT ON (booth_id) booth_id, wait_minutes, congestion_level, updated_at
      FROM booth_congestion ORDER BY booth_id, updated_at DESC, congestion_id DESC) c
WHERE q.booth_id = c.booth_id;
