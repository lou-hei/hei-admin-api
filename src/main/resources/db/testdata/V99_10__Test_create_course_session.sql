insert into "course_session"
(id, awarded_course_id, "begin", "end")
values
    ('course_session1_id', 'awarded_course1_id', '2021-11-08T08:00:00.00Z', '2021-11-08T12:00:00.00Z'),
    ('course_session2_id', 'awarded_course4_id', '2021-08-08T15:00:00.00Z', '2021-08-08T17:00:00.00Z'),
    ('current_course_session_id', 'awarded_course5_id', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);