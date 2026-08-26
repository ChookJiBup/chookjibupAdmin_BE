ALTER TABLE festival_roadmap
    ADD COLUMN IF NOT EXISTS zones jsonb NOT NULL DEFAULT '[]'::jsonb;
