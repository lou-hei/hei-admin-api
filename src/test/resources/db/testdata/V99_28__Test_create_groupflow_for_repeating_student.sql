insert into "group_flow"
(id, group_flow_type, group_id, student_id, flow_datetime)
values
    -- group 1: promotion start datetime: 2021-11-08
    -- group 3: promotion start datetime: 2022-11-08
    -- group 5: promotion start datetime: 2023-11-08

    -- 1. Year 2021: student 7 join group 1
    ('group_flow6_id', 'JOIN', 'group1_id', 'student7_id', '2021-11-08T08:25:24.00Z'),
    -- 3. Year 2022: student 7 leave group 1
    ('group_flow8_id', 'LEAVE', 'group1_id', 'student7_id', '2022-11-08T08:25:24.00Z'),
    -- 5. Year 2022: student 7 join group 3
    ('group_flow10_id', 'JOIN', 'group3_id', 'student7_id', '2022-11-08T08:25:24.00Z'),

    -- 2. Year 2022: student 8 join group 3
    ('group_flow7_id', 'JOIN', 'group3_id', 'student8_id', '2022-11-08T08:25:24.00Z'),
    -- 4. Year 2023: student 8 leave group 3
    ('group_flow9_id', 'LEAVE', 'group3_id', 'student8_id', '2023-11-08T08:25:24.00Z'),
    -- 6. Year 2023 student 8 join group 5
    ('group_flow11_id', 'JOIN', 'group5_id', 'student8_id', '2023-11-08T08:25:24.00Z');