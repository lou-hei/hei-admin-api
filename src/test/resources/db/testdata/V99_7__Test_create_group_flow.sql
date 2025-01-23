insert into "group_flow"
    (id, group_flow_type, group_id, student_id, flow_datetime)
values
    -- NOTE!: this insert is ordered by flow_datetime DESC
    -- 1. student 1,2 join group 1
    ('group_flow1_id', 'JOIN', 'group1_id', 'student1_id', '2021-11-08T08:25:24.00Z'),
    ('group_flow2_id', 'JOIN', 'group1_id', 'student2_id', '2021-11-08T08:25:24.00Z'),
    ('group_flow3_id', 'JOIN', 'group1_id', 'student3_id', '2021-11-08T08:25:24.00Z'),
    -- 2. student 1 joins group 2
    ('group_flow4_id', 'JOIN', 'group2_id', 'student1_id', '2021-11-09T08:25:24.00Z'),
    -- 3. student 3 leaves group 1
    ('group_flow5_id', 'LEAVE', 'group1_id', 'student3_id', '2021-11-09T08:25:24.00Z'),

    ('group_flow20_id', 'JOIN', 'group7_id', 'student9_id', '2021-11-10T08:25:24.00Z'),
    ('group_flow21_id', 'JOIN', 'group6_id', 'student10_id', '2021-11-10T08:25:24.00Z');

-- Group 1: 2 students
-- Group 2: 1 students
-- Group 6: 2 students