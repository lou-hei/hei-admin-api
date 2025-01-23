insert into "fee"
(id, user_id, type, comment,total_amount,  remaining_amount, status,creation_datetime, updated_at, due_datetime)
values
    ('fee98_id', 'student1_id', 'TUITION','Comment', 5000, 3000, 'UNPAID','2021-11-08T08:25:24.00Z','2023-02-08T08:30:24.00Z','2021-09-08T08:25:24.00Z'),
    ('fee99_id', 'student1_id', 'HARDWARE','Comment', 5000, 5000, 'UNPAID','2021-11-10T08:25:24.00Z','2023-02-08T08:30:24.00Z','2021-09-10T08:25:24.00Z');

insert into "payment"
(id, fee_id, type, comment,amount, creation_datetime)
values
    ('payment99_id','fee98_id', 'CASH', 'Comment', 2000, '2022-11-08T08:25:24.00Z');