-- 부스별 대기열(줄끝) 운영 상태. 승인 부스당 최대 1행.
CREATE TABLE IF NOT EXISTS booth_queue (
    queue_id            BIGSERIAL PRIMARY KEY,
    public_id           UUID NOT NULL,
    festival_id         BIGINT NOT NULL REFERENCES festivals (festival_id) ON DELETE CASCADE,
    booth_id            BIGINT NOT NULL REFERENCES booth_info (booth_id) ON DELETE CASCADE,
    tail_latitude       NUMERIC(10, 7),
    tail_longitude      NUMERIC(10, 7),
    queue_tail_meters   INTEGER,
    path_geometry       JSONB,
    modifier_type       VARCHAR(20),
    modifier_admin_id   BIGINT REFERENCES admin_accounts (id) ON DELETE SET NULL,
    modifier_staff_id   BIGINT REFERENCES field_staff_accounts (id) ON DELETE SET NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CLOCK_TIMESTAMP(),
    updated_at          TIMESTAMP NOT NULL DEFAULT CLOCK_TIMESTAMP(),
    CONSTRAINT uk_booth_queue_public_id UNIQUE (public_id),
    CONSTRAINT uk_booth_queue_booth_id UNIQUE (booth_id),
    CONSTRAINT chk_booth_queue_tail_meters CHECK (
        queue_tail_meters IS NULL OR queue_tail_meters >= 0
    ),
    CONSTRAINT chk_booth_queue_modifier CHECK (
        modifier_type IS NULL
        OR (modifier_type = 'ADMIN'
            AND modifier_admin_id IS NOT NULL
            AND modifier_staff_id IS NULL)
        OR (modifier_type = 'STAFF'
            AND modifier_staff_id IS NOT NULL
            AND modifier_admin_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_booth_queue_festival
    ON booth_queue (festival_id);
