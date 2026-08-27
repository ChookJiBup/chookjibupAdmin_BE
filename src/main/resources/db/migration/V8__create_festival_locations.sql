DO $$
BEGIN
    IF to_regclass('festivals') IS NULL THEN
        RETURN;
    END IF;

    IF to_regclass('festival_locations') IS NULL THEN
        CREATE TABLE festival_locations (
            location_id                 BIGSERIAL PRIMARY KEY,
            public_id                   UUID NOT NULL,
            festival_id                 BIGINT NOT NULL,
            location_type               VARCHAR(30) NOT NULL,
            location_name               VARCHAR(150) NOT NULL,
            road_address                VARCHAR(255),
            jibun_address               VARCHAR(255),
            detail_address              VARCHAR(100),
            postal_code                 VARCHAR(10),
            building_management_number  VARCHAR(30),
            latitude                    NUMERIC(10, 7),
            longitude                   NUMERIC(10, 7),
            boundary_geometry           JSONB,
            source_type                 VARCHAR(20) NOT NULL,
            is_primary                  BOOLEAN NOT NULL,
            sort_order                  INTEGER NOT NULL,
            created_by_admin_id         BIGINT,
            last_modified_by_admin_id   BIGINT,
            created_at                  TIMESTAMP NOT NULL,
            updated_at                  TIMESTAMP NOT NULL,
            CONSTRAINT uk_festival_locations_public_id UNIQUE (public_id),
            CONSTRAINT fk_festival_locations_festival
                FOREIGN KEY (festival_id)
                REFERENCES festivals (festival_id)
                ON DELETE CASCADE,
            CONSTRAINT chk_festival_location_coordinates CHECK (
                (latitude IS NULL AND longitude IS NULL)
                OR (latitude IS NOT NULL AND longitude IS NOT NULL)
            ),
            CONSTRAINT chk_festival_location_latitude CHECK (
                latitude IS NULL OR (latitude >= -90 AND latitude <= 90)
            ),
            CONSTRAINT chk_festival_location_longitude CHECK (
                longitude IS NULL OR (longitude >= -180 AND longitude <= 180)
            ),
            CONSTRAINT chk_festival_location_sort_order CHECK (sort_order >= 0),
            CONSTRAINT chk_festival_location_geography CHECK (
                road_address IS NOT NULL
                OR jibun_address IS NOT NULL
                OR latitude IS NOT NULL
                OR boundary_geometry IS NOT NULL
            ),
            CONSTRAINT chk_festival_location_source_admin CHECK (
                (source_type = 'API' AND created_by_admin_id IS NULL)
                OR (source_type = 'MANUAL' AND created_by_admin_id IS NOT NULL)
            )
        );
    ELSE
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'festival_locations'
              AND column_name = 'id'
        ) AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'festival_locations'
              AND column_name = 'location_id'
        ) THEN
            ALTER TABLE festival_locations RENAME COLUMN id TO location_id;
        END IF;

        ALTER TABLE festival_locations
            ADD COLUMN IF NOT EXISTS public_id UUID,
            ADD COLUMN IF NOT EXISTS festival_id BIGINT,
            ADD COLUMN IF NOT EXISTS location_type VARCHAR(30),
            ADD COLUMN IF NOT EXISTS location_name VARCHAR(150),
            ADD COLUMN IF NOT EXISTS road_address VARCHAR(255),
            ADD COLUMN IF NOT EXISTS jibun_address VARCHAR(255),
            ADD COLUMN IF NOT EXISTS detail_address VARCHAR(100),
            ADD COLUMN IF NOT EXISTS postal_code VARCHAR(10),
            ADD COLUMN IF NOT EXISTS building_management_number VARCHAR(30),
            ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 7),
            ADD COLUMN IF NOT EXISTS longitude NUMERIC(10, 7),
            ADD COLUMN IF NOT EXISTS boundary_geometry JSONB,
            ADD COLUMN IF NOT EXISTS source_type VARCHAR(20),
            ADD COLUMN IF NOT EXISTS is_primary BOOLEAN,
            ADD COLUMN IF NOT EXISTS sort_order INTEGER,
            ADD COLUMN IF NOT EXISTS created_by_admin_id BIGINT,
            ADD COLUMN IF NOT EXISTS last_modified_by_admin_id BIGINT,
            ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
            ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

        UPDATE festival_locations
        SET public_id = MD5(
                location_id::TEXT || ':' ||
                RANDOM()::TEXT || ':' ||
                CLOCK_TIMESTAMP()::TEXT
        )::UUID
        WHERE public_id IS NULL;

        UPDATE festival_locations
        SET location_type = COALESCE(location_type, 'MAIN_VENUE'),
            location_name = COALESCE(NULLIF(BTRIM(location_name), ''), '장소'),
            source_type = COALESCE(source_type, 'MANUAL'),
            is_primary = COALESCE(is_primary, FALSE),
            sort_order = COALESCE(sort_order, 0),
            created_at = COALESCE(created_at, CLOCK_TIMESTAMP()),
            updated_at = COALESCE(updated_at, CLOCK_TIMESTAMP());

        UPDATE festival_locations
        SET road_address = COALESCE(
                road_address,
                NULLIF(BTRIM(location_name), ''),
                '주소 미상'
        )
        WHERE road_address IS NULL
          AND jibun_address IS NULL
          AND latitude IS NULL
          AND boundary_geometry IS NULL;

        UPDATE festival_locations
        SET created_by_admin_id = NULL
        WHERE source_type = 'API'
          AND created_by_admin_id IS NOT NULL;

        IF EXISTS (
            SELECT 1
            FROM festival_locations
            WHERE source_type = 'MANUAL'
              AND created_by_admin_id IS NULL
        ) THEN
            RAISE EXCEPTION
                'festival_locations MANUAL rows require created_by_admin_id before chk_festival_location_source_admin';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM festival_locations
            WHERE festival_id IS NULL
               OR public_id IS NULL
               OR location_type IS NULL
               OR location_name IS NULL
               OR source_type IS NULL
               OR is_primary IS NULL
               OR sort_order IS NULL
               OR created_at IS NULL
               OR updated_at IS NULL
        ) THEN
            RAISE EXCEPTION
                'festival_locations backfill incomplete before NOT NULL constraints';
        END IF;

        ALTER TABLE festival_locations
            ALTER COLUMN public_id SET NOT NULL,
            ALTER COLUMN festival_id SET NOT NULL,
            ALTER COLUMN location_type SET NOT NULL,
            ALTER COLUMN location_name SET NOT NULL,
            ALTER COLUMN source_type SET NOT NULL,
            ALTER COLUMN is_primary SET NOT NULL,
            ALTER COLUMN sort_order SET NOT NULL,
            ALTER COLUMN created_at SET NOT NULL,
            ALTER COLUMN updated_at SET NOT NULL;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'uk_festival_locations_public_id'
        ) THEN
            ALTER TABLE festival_locations
                ADD CONSTRAINT uk_festival_locations_public_id UNIQUE (public_id);
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_festival_locations_festival'
        ) THEN
            ALTER TABLE festival_locations
                ADD CONSTRAINT fk_festival_locations_festival
                FOREIGN KEY (festival_id)
                REFERENCES festivals (festival_id)
                ON DELETE CASCADE;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'chk_festival_location_coordinates'
        ) THEN
            ALTER TABLE festival_locations
                ADD CONSTRAINT chk_festival_location_coordinates CHECK (
                    (latitude IS NULL AND longitude IS NULL)
                    OR (latitude IS NOT NULL AND longitude IS NOT NULL)
                );
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'chk_festival_location_latitude'
        ) THEN
            ALTER TABLE festival_locations
                ADD CONSTRAINT chk_festival_location_latitude CHECK (
                    latitude IS NULL OR (latitude >= -90 AND latitude <= 90)
                );
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'chk_festival_location_longitude'
        ) THEN
            ALTER TABLE festival_locations
                ADD CONSTRAINT chk_festival_location_longitude CHECK (
                    longitude IS NULL OR (longitude >= -180 AND longitude <= 180)
                );
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'chk_festival_location_sort_order'
        ) THEN
            ALTER TABLE festival_locations
                ADD CONSTRAINT chk_festival_location_sort_order CHECK (sort_order >= 0);
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'chk_festival_location_geography'
        ) THEN
            ALTER TABLE festival_locations
                ADD CONSTRAINT chk_festival_location_geography CHECK (
                    road_address IS NOT NULL
                    OR jibun_address IS NOT NULL
                    OR latitude IS NOT NULL
                    OR boundary_geometry IS NOT NULL
                );
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'chk_festival_location_source_admin'
        ) THEN
            ALTER TABLE festival_locations
                ADD CONSTRAINT chk_festival_location_source_admin CHECK (
                    (source_type = 'API' AND created_by_admin_id IS NULL)
                    OR (source_type = 'MANUAL' AND created_by_admin_id IS NOT NULL)
                );
        END IF;
    END IF;

    CREATE INDEX IF NOT EXISTS idx_festival_locations_festival
        ON festival_locations (festival_id);

    CREATE INDEX IF NOT EXISTS idx_festival_locations_source_primary
        ON festival_locations (festival_id, source_type, is_primary);
END
$$;
