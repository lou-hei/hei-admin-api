insert into "work_document"
(id, filename, creation_datetime, commitment_begin, file_path, student_id)
values ('work_file1_id', 'work file', '2021-11-08T08:25:24.00Z', '2021-11-08T08:25:24.00Z', 'STUDENT/STD21001/WORK_DOCUMENT/STD21001.pdf', 'student1_id'),
       ('work_file2_id', 'work file 2', '2021-12-08T08:25:24.00Z', '2021-11-08T08:25:24.00Z', 'STUDENT/STD21002/WORK_DOCUMENT/STD21001_2.pdf', 'student2_id');

insert into "work_document"
(id, filename, creation_datetime, commitment_begin, file_path, student_id, professional_experience_type)
values ('work_file3_id', 'business file', '2020-11-08T08:25:24.00Z', '2020-11-08T08:25:24.00Z', 'STUDENT/STD21001/WORK_DOCUMENT/STD21001.pdf', 'student1_id', 'BUSINESS_OWNER'),
       ('work_file4_id', 'business file 2', '2020-12-08T08:25:24.00Z', '2020-11-08T08:25:24.00Z', 'STUDENT/STD21002/WORK_DOCUMENT/STD21001_2.pdf', 'student2_id', 'BUSINESS_OWNER');

insert into "work_document"
(id, filename, creation_datetime, commitment_begin, file_path, student_id, professional_experience_type)
values ('work_file5_id', 'intern file', '2020-11-08T08:25:24.00Z', '2020-11-08T08:25:24.00Z', 'STUDENT/STD21001/WORK_DOCUMENT/STD21001.pdf', 'student1_id', 'INTERN_STUDENT');
