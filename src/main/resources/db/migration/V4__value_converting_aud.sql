CREATE TABLE value_converting_aud
(
    id                  BIGINT   NOT NULL,
    rev                 BIGINT   NOT NULL REFERENCES revinfo (rev),
    revtype             SMALLINT,
    display_name        VARCHAR(255),
    from_application_id INT8,
    from_type_id        VARCHAR(255),
    to_application_id   VARCHAR(255),
    to_type_id          VARCHAR(255),
    PRIMARY KEY (id, rev)
);

CREATE TABLE converting_map_aud
(
    value_converting_id INT8     NOT NULL,
    rev                 BIGINT   NOT NULL REFERENCES revinfo (rev),
    revtype             SMALLINT,
    key                 VARCHAR(255) NOT NULL,
    value               VARCHAR(255),
    PRIMARY KEY (value_converting_id, rev, key)
);
