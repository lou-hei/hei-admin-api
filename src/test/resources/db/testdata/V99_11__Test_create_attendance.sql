insert into "attendance"
(id, created_at, is_late, student_id, course_session_id, place, attendance_movement_type)
values
    ('attendance1_id', '2021-11-08T07:30:00.00Z', false, 'student1_id', 'course_session1_id', 'ANDRAHARO', 'IN'),
    ('attendance2_id', '2021-08-08T14:15:00.00Z', false, 'student1_id', 'course_session2_id', 'ANDRAHARO', 'IN'),
    ('attendance3_id', '2021-11-08T08:35:00.00Z', true, 'student2_id', 'course_session1_id', 'IVANDRY', 'IN'),
    ('attendance4_id', '2021-08-08T15:15:00.00Z', true, 'student2_id', 'course_session2_id', 'ANDRAHARO', 'IN'),
    ('attendance5_id', null, false, 'student3_id', 'course_session1_id', null, 'IN'),
    ('attendance6_id', null, false, 'student3_id', 'course_session2_id', null, 'IN'),
    ('attendance7_id', '2021-11-08T012:30:00.00Z', false, 'student1_id', 'course_session1_id', 'ANDRAHARO', 'OUT'),
    ('attendance8_id', '2021-11-08T012:00:00.00Z', false, 'student2_id', 'course_session1_id', 'IVANDRY', 'OUT'),
    ('attendance9_id', CURRENT_TIMESTAMP, false, 'student3_id', 'current_course_session_id', 'IVANDRY', 'IN');