insert into "user"
(id, group_id,picture, first_name, last_name, email, ref, status, sex, birth_date, entrance_datetime, phone, address,
 "role")
values ('student1_id', 'group1_id','student1.jpg','Ryan', 'Andria', 'test+ryan@hei.school', 'STD21001', 'ENABLED', 'M',
        '2000-01-01',
        '2021-11-08T08:25:24.00Z', '0322411123', 'Adr 1', 'STUDENT'),
       ('student2_id', 'group2_id','student2.jpg', 'Two', 'Student', 'test+student2@hei.school', 'STD21002', 'ENABLED', 'F',
        '2000-01-02',
        '2021-11-09T08:26:24.00Z', '0322411124', 'Adr 2', 'STUDENT'),
       ('student3_id', 'group1_id','student3.jpg', 'Three', 'Student', 'test+student3@hei.school', 'STD21003', 'ENABLED', 'F',
        '2000-01-02',
        '2021-11-09T08:26:24.00Z', '0322411124', 'Adr 2', 'STUDENT');
insert into "user"
(id, first_name, picture, last_name, email, ref, status, sex, birth_date, entrance_datetime, phone, address,
 "role")
values ('teacher1_id', 'One','teacher1.jpg', 'Teacher', 'test+teacher1@hei.school', 'TCR21001', 'ENABLED', 'F',
        '1990-01-01',
        '2021-10-08T08:27:24.00Z', '0322411125', 'Adr 3', 'TEACHER'),
       ('teacher2_id', 'Two','teacher2.jpg', 'Teacher', 'test+teacher2@hei.school', 'TCR21002', 'ENABLED', 'M',
        '1990-01-02',
        '2021-10-09T08:28:24.00Z', '0322411126', 'Adr 4', 'TEACHER'),
       ('teacher3_id', 'Three','teacher3.jpg', 'Teach', 'test+teacher3@hei.school', 'TCR21003', 'ENABLED', 'M',
        '1990-01-02',
        '2021-10-09T08:28:24.00Z', '0322411126', 'Adr 4', 'TEACHER'),
       ('manager1_id', 'One','toky1.jpg' ,'Manager', 'test+manager1@hei.school', 'MGR21001', 'ENABLED', 'M',
        '1890-01-01',
        '2021-09-08T08:25:29.00Z', '0322411127', 'Adr 5', 'MANAGER');

