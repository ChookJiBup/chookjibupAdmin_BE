DO $$
BEGIN
    IF to_regclass('festival_series') IS NULL THEN
        CREATE TABLE festival_series (
            series_id       BIGSERIAL PRIMARY KEY,
            public_id       UUID NOT NULL,
            series_name     VARCHAR(100) NOT NULL,
            normalized_name VARCHAR(100) NOT NULL,
            created_at      TIMESTAMP NOT NULL,
            updated_at      TIMESTAMP NOT NULL,
            CONSTRAINT uk_festival_series_public_id UNIQUE (public_id),
            CONSTRAINT uk_festival_series_normalized_name UNIQUE (normalized_name)
        );
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'festival_series'
          AND column_name = 'id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'festival_series'
          AND column_name = 'series_id'
    ) THEN
        ALTER TABLE festival_series RENAME COLUMN id TO series_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'festival_series'
          AND column_name = 'name'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'festival_series'
          AND column_name = 'series_name'
    ) THEN
        ALTER TABLE festival_series RENAME COLUMN name TO series_name;
    END IF;

    ALTER TABLE festival_series
        ADD COLUMN IF NOT EXISTS public_id UUID,
        ADD COLUMN IF NOT EXISTS series_name VARCHAR(100),
        ADD COLUMN IF NOT EXISTS normalized_name VARCHAR(100),
        ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

    UPDATE festival_series
    SET public_id = MD5(
            series_id::TEXT || ':' ||
            RANDOM()::TEXT || ':' ||
            CLOCK_TIMESTAMP()::TEXT
    )::UUID
    WHERE public_id IS NULL;

    UPDATE festival_series
    SET series_name = COALESCE(
            NULLIF(BTRIM(series_name), ''),
            'festival-series-' || series_id::TEXT
    )
    WHERE series_name IS NULL
       OR BTRIM(series_name) = '';

    UPDATE festival_series
    SET normalized_name = lower(regexp_replace(series_name, '\s+', '', 'g'))
    WHERE normalized_name IS NULL
       OR BTRIM(normalized_name) = '';

    -- 동일 normalized_name 중복은 series_id를 붙여 UNIQUE를 맞춘다.
    UPDATE festival_series fs
    SET normalized_name = fs.normalized_name || '-' || fs.series_id::TEXT
    WHERE EXISTS (
        SELECT 1
        FROM festival_series other
        WHERE other.normalized_name = fs.normalized_name
          AND other.series_id < fs.series_id
    );

    UPDATE festival_series
    SET created_at = COALESCE(created_at, CLOCK_TIMESTAMP()),
        updated_at = COALESCE(updated_at, CLOCK_TIMESTAMP())
    WHERE created_at IS NULL
       OR updated_at IS NULL;

    IF EXISTS (
        SELECT 1
        FROM festival_series
        WHERE series_name IS NULL
           OR normalized_name IS NULL
           OR public_id IS NULL
           OR created_at IS NULL
           OR updated_at IS NULL
    ) THEN
        RAISE EXCEPTION
            'festival_series backfill incomplete before NOT NULL constraints';
    END IF;

    ALTER TABLE festival_series
        ALTER COLUMN public_id SET NOT NULL,
        ALTER COLUMN series_name SET NOT NULL,
        ALTER COLUMN normalized_name SET NOT NULL,
        ALTER COLUMN created_at SET NOT NULL,
        ALTER COLUMN updated_at SET NOT NULL;

    CREATE UNIQUE INDEX IF NOT EXISTS uk_festival_series_public_id
        ON festival_series (public_id);

    CREATE UNIQUE INDEX IF NOT EXISTS uk_festival_series_normalized_name
        ON festival_series (normalized_name);
END
$$;
