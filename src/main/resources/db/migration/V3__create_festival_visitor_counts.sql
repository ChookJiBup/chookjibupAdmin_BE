CREATE TABLE IF NOT EXISTS festival_visitor_count (
    visitor_count_id BIGSERIAL PRIMARY KEY,
    festival_id      BIGINT NOT NULL REFERENCES festivals (festival_id) ON DELETE CASCADE,
    visit_date       DATE NOT NULL,
    visitor_count    INTEGER NOT NULL DEFAULT 0 CHECK (visitor_count >= 0),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_visitor_count_festival_date UNIQUE (festival_id, visit_date)
);

CREATE INDEX IF NOT EXISTS idx_visitor_count_festival
    ON festival_visitor_count (festival_id);

ALTER TABLE IF EXISTS festival_visitor_count
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE festival_visitor_count
SET visitor_count = 0
WHERE visitor_count IS NULL;

ALTER TABLE IF EXISTS festival_visitor_count
    ALTER COLUMN visitor_count SET NOT NULL;

CREATE TABLE IF NOT EXISTS festival_visitor_total (
    visitor_total_id    BIGSERIAL PRIMARY KEY,
    festival_id         BIGINT NOT NULL REFERENCES festivals (festival_id) ON DELETE CASCADE,
    total_visitor_count INTEGER NOT NULL CHECK (total_visitor_count >= 0),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_visitor_total_festival UNIQUE (festival_id)
);

CREATE INDEX IF NOT EXISTS idx_visitor_total_festival
    ON festival_visitor_total (festival_id);
