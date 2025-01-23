insert into "event"(id, type, title, description, "begin_datetime", "end_datetime", planner_id, course_id)
values('event1_id', 'COURSE','PROG1', 'Prog1 course', '2022-12-20T08:00:00.00Z', '2022-12-20T10:00:00.00Z', 'manager1_id', 'course1_id'),
      ('event2_id', 'INTEGRATION','Integration Day', 'HEI students integration day', '2022-12-08T08:00:00.00Z', '2022-12-08T12:00:00.00Z', 'manager1_id', null),
      ('event3_id', 'SEMINAR','December Seminar', 'Seminar about Python programming language', '2022-12-09T08:00:00.00Z', '2022-12-09T12:00:00.00Z', 'manager10_id', null);

insert into "event_participant"(id, event_id, participant_id, status, group_id)
values ('event_participant1_id', 'event1_id', 'student1_id', 'MISSING', 'group1_id'),
       ('event_participant2_id', 'event1_id', 'student3_id', 'PRESENT', 'group1_id'),
       ('event_participant3_id', 'event2_id', 'student1_id', 'PRESENT', 'group1_id'),
       ('event_participant4_id', 'event2_id', 'student2_id', 'PRESENT','group2_id'),
       ('event_participant5_id', 'event2_id', 'student3_id', 'MISSING', 'group1_id');

insert into "event_group_participate"(id, event_id, group_id)
values ('group_participate1_id', 'event1_id', 'group1_id'),
       ('group_participate2_id', 'event2_id', 'group1_id'),
       ('group_participate3_id', 'event2_id', 'group2_id');