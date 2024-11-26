insert into "user"
(id, first_name, last_name, email, ref, status, sex, birth_date, entrance_datetime, phone, address,
 "role", birth_place, nic, longitude, latitude, high_school_origin)
values ('student1_id', 'Ryan', 'Andria', 'test+ryan@hei.school', 'STD21001', 'ENABLED', 'M',
        '2000-01-01',
        '2021-11-08T08:25:24.00Z', '0322411123', 'Adr 1', 'STUDENT', '', '', -123.123, 123.0, 'Lycée Andohalo'),
       ('student2_id', 'Two', 'Student', 'test+student2@hei.school', 'STD21002', 'ENABLED', 'F',
        '2000-01-02',
        '2021-11-09T08:26:24.00Z', '0322411124', 'Adr 2', 'STUDENT', '', '', 255.255, -255.255, 'Lycée Andohalo'),
       ('student3_id', 'Three', 'Student', 'test+student3@hei.school', 'STD21003', 'ENABLED', 'F',
        '2000-01-02',
        '2021-11-09T08:26:24.00Z', '0322411124', 'Adr 2', 'STUDENT', 'Befelatanana', '0000000000', null, null, 'Lycée Analamahitsy'),
       ('teacher1_id', 'One', 'Teacher', 'test+teacher1@hei.school', 'TCR21001', 'ENABLED', 'F',
        '1990-01-01',
        '2021-10-08T08:27:24.00Z', '0322411125', 'Adr 3', 'TEACHER', '', '', 999.999, 999.999, null),
       ('teacher2_id', 'Two', 'Teacher', 'test+teacher2@hei.school', 'TCR21002', 'ENABLED', 'M',
        '1990-01-02',
        '2021-10-09T08:28:24.00Z', '0322411126', 'Adr 4', 'TEACHER', '', '', null, null, null),
       ('teacher3_id', 'Three', 'Teach', 'test+teacher3@hei.school', 'TCR21003', 'ENABLED', 'M',
        '1990-01-02',
        '2021-10-09T08:28:24.00Z', '0322411126', 'Adr 4', 'TEACHER', '', '', null, null, null),
       ('teacher4_id', 'Four', 'Binary', 'test+teacher4@hei.school', 'TCR21004', 'ENABLED', 'F',
        '1990-01-04',
        '2021-10-09T08:28:24.00Z', '0322411426', 'Adr 5', 'TEACHER', '', '', null, null, null),
       ('manager1_id', 'One', 'Manager', 'test+manager1@hei.school', 'MGR21001', 'ENABLED', 'M',
        '1890-01-01',
        '2021-09-08T08:25:29.00Z', '0322411127', 'Adr 5', 'MANAGER', '', '', 55.555, -55.555, null),
        ('manager10_id', 'Two', 'Manager', 'test+manager2@hei.school', 'MGR21002', 'ENABLED', 'M',
         '1890-01-01',
         '2021-09-08T08:25:29.00Z', '0322411128', 'Adr 5', 'MANAGER', '', '', 55.555, -55.555, null),
       ('admin1_id', 'Admin', 'Admin', 'test+admin@hei.school', 'ADM21001', 'ENABLED', 'M',
        '1890-01-01',
        '2021-09-08T08:25:29.00Z', '0322411128', 'Adr 5', 'ADMIN', '', '', 55.555, -55.555, null),
        ('staff1_id', 'Staff', 'staff', 'test+staff@hei.school', 'STF21001', 'ENABLED', 'M',
        '1890-01-01',
        '2021-09-08T08:25:29.00Z', '0322411128', 'Adr 5', 'ADMIN', '', '', 55.555, -55.555, null)
    ;
