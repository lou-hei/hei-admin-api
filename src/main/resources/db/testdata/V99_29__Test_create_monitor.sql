insert into "user"
(id, first_name, last_name, email, ref, status, sex, birth_date, entrance_datetime, phone, address,
 "role", birth_place, nic, longitude, latitude, high_school_origin)
values ('monitor1_id', 'Monitor', 'One', 'test+monitor@hei.school', 'MTR21001', 'ENABLED', 'M',
        '2000-01-01',
        '2021-11-08T08:25:24.00Z', '0322411123', 'Adr 1', 'MONITOR', '', '', -123.123, 123.0, 'Lycée Andohalo'),
    ('monitor2_id', 'Monitor2', 'two', 'test+monitor2@hei.school', 'MTR21002', 'ENABLED', 'M',
        '2000-02-02',
        '2021-11-08T08:25:24.00Z', '0322411123', 'Adr 2', 'MONITOR', '', '', -123.123, 123.0, 'Lycée Andohalo');