update "promotion" set start_datetime = '2021-11-08' where id = 'promotion1_id';
update "promotion" set start_datetime = '2022-11-08' where id = 'promotion2_id';
update "promotion" set start_datetime = '2023-11-08' where id = 'promotion3_id';
update "promotion" set start_datetime = '2025-11-08' where id = 'promotion4_id';
update "promotion" set start_datetime = '2024-11-08' where id = 'promotion5_id';
update "group" set promotion_id = 'promotion3_id' where id = 'group5_id';