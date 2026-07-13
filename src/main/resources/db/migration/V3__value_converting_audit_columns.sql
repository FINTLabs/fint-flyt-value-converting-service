ALTER TABLE value_converting
    ADD COLUMN created_at       TIMESTAMPTZ NULL,
    ADD COLUMN created_by       JSONB NOT NULL DEFAULT '{"type":"UNKNOWN"}'::jsonb,
    ADD COLUMN last_modified_at TIMESTAMPTZ NULL,
    ADD COLUMN last_modified_by JSONB NOT NULL DEFAULT '{"type":"UNKNOWN"}'::jsonb;
