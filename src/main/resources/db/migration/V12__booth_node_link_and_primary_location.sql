DO $$
BEGIN
    -- 부스 ↔ 지도 노드 유일 연결
    IF to_regclass('booth_info') IS NOT NULL THEN
        ALTER TABLE booth_info
            ADD COLUMN IF NOT EXISTS roadmap_node_id BIGINT;

        IF to_regclass('roadmap_node') IS NOT NULL THEN
            UPDATE booth_info bi
            SET roadmap_node_id = rn.id
            FROM roadmap_node rn
            WHERE bi.roadmap_node_id IS NULL
              AND rn.related_booth_id = bi.booth_id;

            IF NOT EXISTS (
                SELECT 1 FROM pg_constraint WHERE conname = 'fk_booth_info_roadmap_node'
            ) AND EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = 'roadmap_node'
                  AND column_name = 'id'
            ) THEN
                ALTER TABLE booth_info
                    ADD CONSTRAINT fk_booth_info_roadmap_node
                    FOREIGN KEY (roadmap_node_id)
                    REFERENCES roadmap_node (id)
                    ON DELETE SET NULL;
            END IF;
        END IF;

        CREATE UNIQUE INDEX IF NOT EXISTS uk_booth_info_festival_roadmap_node
            ON booth_info (festival_id, roadmap_node_id)
            WHERE roadmap_node_id IS NOT NULL;
    END IF;

    -- 축제당 primary 주소 1개 보장 (기존 중복은 최소 location_id만 유지)
    IF to_regclass('festival_locations') IS NOT NULL THEN
        UPDATE festival_locations fl
        SET is_primary = FALSE
        WHERE fl.is_primary = TRUE
          AND fl.location_id NOT IN (
                SELECT kept.location_id
                FROM (
                    SELECT DISTINCT ON (festival_id) location_id
                    FROM festival_locations
                    WHERE is_primary = TRUE
                    ORDER BY festival_id, location_id
                ) kept
          );

        CREATE UNIQUE INDEX IF NOT EXISTS uk_festival_locations_one_primary
            ON festival_locations (festival_id)
            WHERE is_primary = TRUE;
    END IF;
END
$$;
