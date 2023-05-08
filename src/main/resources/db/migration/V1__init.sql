create table converting_map
(
    value_converting_id int8         not null,
    value               varchar(255),
    key                 varchar(255) not null,
    primary key (value_converting_id, key)
);
create table value_converting
(
    id                  bigserial not null,
    display_name        varchar(255),
    from_application_id int8,
    from_type_id        varchar(255),
    to_application_id   varchar(255),
    to_type_id          varchar(255),
    primary key (id)
);
alter table converting_map
    add constraint FKgmykv0l5f3xmv7u6rxf2ocik8 foreign key (value_converting_id) references value_converting;
