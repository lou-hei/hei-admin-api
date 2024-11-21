insert into "file_info"
(id, user_id, name, creation_datetime, file_type, file_path)
values
    ('file1_id', 'student1_id', 'transcript1', '2021-11-08T08:25:24.00Z', 'TRANSCRIPT', 'STUDENT/TRANSCRIPT/STD21001/2021.pdf'),
    ('file2_id', 'student1_id', 'other1', '2021-11-10T08:25:24.00Z', 'OTHER', 'STUDENT/OTHER/STD21001/other.pdf'),
    ('file3_id', null, 'school_file', '2021-11-08T08:25:24.00Z', 'DOCUMENT', 'SCHOOL_FILES/school_file.pdf'),
    ('file4_id', 'teacher1_id', 'school_file', '2021-11-08T08:25:24.00Z', 'OTHER', 'TEACHER/OTHER/TCR21001/school_file.pdf');