insert into "payment"
    (id, fee_id, type, comment,amount, creation_datetime)
values
    ('payment1_id','fee1_id', 'CASH', 'Comment', 2000, '2022-11-08T08:25:24.00Z'),
    ('payment2_id','fee1_id', 'MOBILE_MONEY', null, 3000, '2022-11-10T08:25:25.00Z'),
    ('payment3_id','fee2_id', 'CASH', 'Comment', 5000, '2022-11-11T08:25:26.00Z'),
    ('payment4_id','fee4_id', 'SCHOLARSHIP', null, 5000, '2022-11-12T08:25:26.00Z'),
    ('payment5_id','fee5_id', 'FIX', 'Comment', 5000, '2022-11-12T08:25:26.00Z'),
    ('payment6_id','fee1_id', 'CASH', 'Comment', 0, '2022-11-15T08:25:25.00Z');