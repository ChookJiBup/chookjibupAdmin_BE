DO $$
BEGIN
    -- 승인된 지도 부스 마스터
    IF to_regclass('booth_info') IS NULL THEN
        CREATE TABLE booth_info (
            booth_id     BIGSERIAL PRIMARY KEY,
            festival_id  BIGINT NOT NULL REFERENCES festivals (festival_id) ON DELETE CASCADE,
            roadmap_node_id BIGINT,
            booth_name   TEXT NOT NULL,
            booth_content TEXT,
            booth_location TEXT,
            created_at   TIMESTAMP NOT NULL DEFAULT CLOCK_TIMESTAMP(),
            updated_at   TIMESTAMP NOT NULL DEFAULT CLOCK_TIMESTAMP()
        );
        CREATE INDEX idx_booth_info_festival ON booth_info (festival_id);
        CREATE UNIQUE INDEX uk_booth_info_festival_roadmap_node
            ON booth_info (festival_id, roadmap_node_id)
            WHERE roadmap_node_id IS NOT NULL;
    END IF;

    -- 부스 혼잡 이력 (Admin BE: modifier_staff → field_staff_accounts)
    IF to_regclass('booth_congestion') IS NULL THEN
        CREATE TABLE booth_congestion (
            congestion_id       BIGSERIAL PRIMARY KEY,
            booth_id            BIGINT NOT NULL REFERENCES booth_info (booth_id) ON DELETE CASCADE,
            modifier_type       VARCHAR(20) NOT NULL,
            modifier_admin_id   BIGINT REFERENCES admin_accounts (id) ON DELETE SET NULL,
            modifier_staff_id   BIGINT REFERENCES field_staff_accounts (id) ON DELETE SET NULL,
            wait_minutes        INTEGER CHECK (wait_minutes IS NULL OR wait_minutes >= 0),
            congestion_level    VARCHAR(20) NOT NULL,
            created_at          TIMESTAMP NOT NULL DEFAULT CLOCK_TIMESTAMP(),
            updated_at          TIMESTAMP NOT NULL DEFAULT CLOCK_TIMESTAMP(),
            CONSTRAINT chk_booth_congestion_modifier CHECK (
                (modifier_type = 'ADMIN'
                    AND modifier_admin_id IS NOT NULL
                    AND modifier_staff_id IS NULL)
                OR (modifier_type = 'STAFF'
                    AND modifier_staff_id IS NOT NULL
                    AND modifier_admin_id IS NULL)
            )
        );
    ELSE
        -- 기존 festival_staff FK가 있으면 field_staff_accounts로 교체
        IF EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_schema = current_schema()
              AND table_name = 'booth_congestion'
              AND constraint_name = 'booth_congestion_modifier_staff_id_fkey'
        ) THEN
            ALTER TABLE booth_congestion
                DROP CONSTRAINT booth_congestion_modifier_staff_id_fkey;
        END IF;

        -- 파이프라인 enum/소문자 modifier 호환 컬럼이 VARCHAR가 아니면 최소 보정은 운영에서 수동 검토
        IF to_regclass('field_staff_accounts') IS NOT NULL
           AND NOT EXISTS (
                SELECT 1
                FROM pg_constraint
                WHERE conname = 'fk_booth_congestion_modifier_staff_field'
           ) THEN
            ALTER TABLE booth_congestion
                ADD CONSTRAINT fk_booth_congestion_modifier_staff_field
                FOREIGN KEY (modifier_staff_id)
                REFERENCES field_staff_accounts (id)
                ON DELETE SET NULL;
        END IF;
    END IF;

    CREATE INDEX IF NOT EXISTS idx_booth_congestion_latest
        ON booth_congestion (booth_id, created_at DESC);

    -- 지도 노드 ↔ 승인 부스
    IF to_regclass('roadmap_node') IS NOT NULL THEN
        ALTER TABLE roadmap_node
            ADD COLUMN IF NOT EXISTS related_booth_id BIGINT;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_roadmap_node_related_booth'
        ) AND to_regclass('booth_info') IS NOT NULL THEN
            ALTER TABLE roadmap_node
                ADD CONSTRAINT fk_roadmap_node_related_booth
                FOREIGN KEY (related_booth_id)
                REFERENCES booth_info (booth_id)
                ON DELETE SET NULL;
        END IF;
    END IF;
END
$$;
