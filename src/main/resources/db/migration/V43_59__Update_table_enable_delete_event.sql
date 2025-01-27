-- alter table event
alter table "event" add column if not exists is_deleted boolean default false;
