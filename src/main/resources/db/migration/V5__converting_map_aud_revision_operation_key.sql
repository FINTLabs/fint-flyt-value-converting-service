ALTER TABLE converting_map_aud
    DROP CONSTRAINT converting_map_aud_pkey;

ALTER TABLE converting_map_aud
    ALTER COLUMN revtype SET NOT NULL;

ALTER TABLE converting_map_aud
    ADD CONSTRAINT converting_map_aud_pkey PRIMARY KEY (value_converting_id, rev, key, revtype);
