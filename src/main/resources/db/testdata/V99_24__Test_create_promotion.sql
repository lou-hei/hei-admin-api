insert into "promotion"(id, name, ref, creation_datetime)
values ('promotion1_id', 'Promotion 21-22', 'PROM21', '2021-11-08T08:30:24.00Z'),
       ('promotion2_id', 'Promotion 22-23', 'PROM22', '2021-11-08T08:30:24.00Z'),
       ('promotion3_id', 'Promotion 23-24', 'PROM23', '2021-11-08T08:30:24.00Z'),
       ('promotion4_id', 'Promotion 25-26', 'PROM24', '2021-11-08T08:30:24.00Z'),
       ('promotion5_id', 'Promotion 26-27', 'PROM25', '2020-11-08T08:30:24.00Z');

update "group"
set promotion_id = 'promotion1_id'
where id = 'group1_id'
   or id = 'group2_id';
update "group"
set promotion_id = 'promotion2_id'
where id = 'group3_id'
   or id = 'group4_id';
update "group"
set promotion_id = 'promotion4_id'
where id = 'group6_id';
update "group"
set promotion_id = 'promotion5_id'
where id = 'group7_id';
