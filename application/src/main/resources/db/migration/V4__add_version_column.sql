alter table pre_shared_key
    add column version int default 0;

alter table pre_shared_key
    drop constraint pre_shared_key_pkey;
alter table pre_shared_key
    add primary key (identity, version);
