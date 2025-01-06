alter table "user" add column if not exists "cnaps" varchar;
alter table "user" add column if not exists "ostie" varchar;
alter table "user" add column if not exists "ending_service" timestamp with time zone;