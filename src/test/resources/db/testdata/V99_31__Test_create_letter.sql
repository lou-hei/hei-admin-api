INSERT INTO letter (id, description, creation_datetime, approval_datetime, student_id, file_path, status, "ref", reason_for_refusal)
VALUES
    ('letter1_id', 'Certificat de residence', '2021-11-08T08:25:24.00Z', '2021-12-08T08:25:24.00Z', 'student1_id', '/LETTERBOX/STD21001/file1.pdf', 'RECEIVED', 'letter1_ref', null),
    ('letter2_id', 'Bordereau de versement', '2021-11-08T08:25:24.00Z', NULL, 'student1_id', '/LETTERBOX/STD21001/file2.pdf', 'PENDING', 'letter2_ref', null),
    ('letter3_id', 'CV', '2021-11-08T08:25:24.00Z', NULL, 'student2_id', '/LETTERBOX/STD21002/file3.pdf', 'PENDING', 'letter3_ref', null),
    ('letter4_id', 'Rejected file', '2021-11-08T08:25:24.00Z', NULL, 'student2_id', '/LETTERBOX/STD21002/file3.pdf', 'PENDING', 'letter4_ref', 'Mauvais format de fichier'),
    ('letter6_id', 'Rejected file', '2021-11-08T08:25:24.00Z', NULL, 'staff1_id', '/LETTERBOX/STD21002/file3.pdf', 'PENDING', 'letter6_ref', 'Mauvais format de fichier'),
    ('letter7_id', 'Rejected file', '2021-11-08T08:25:24.00Z', NULL, 'staff2_id', '/LETTERBOX/STD21002/file3.pdf', 'PENDING', 'letter7_ref', 'Mauvais format de fichier'),
    ('letter5_id', 'Teacher file', '2021-11-08T08:25:24.00Z', NULL, 'teacher1_id', '/LETTERBOX/STD21002/file3.pdf', 'RECEIVED', 'letter5_ref', 'fichier');