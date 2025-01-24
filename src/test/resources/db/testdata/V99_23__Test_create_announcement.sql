insert into announcement(id, title, content, author_id, creation_datetime,"scope")
values ('announcement1_id', 'Fermeture du bureau', 'Le bureau est fermé pour ce weekend', 'manager1_id', '2022-12-20T08:00:00.00Z', 'GLOBAL'),
       ('announcement2_id', 'Congé autorisé', 'A tous les enseignants, vous êtes disposés à prendre des congés', 'manager1_id', '2022-12-21T08:00:00.00Z', 'TEACHER'),
       ('announcement3_id', 'Cours annulé pour G1', 'Les cours des G1 sont annulés pour demain', 'teacher1_id', '2022-12-22T08:00:00.00Z', 'STUDENT'),
       ('announcement4_id', 'Comptabilité', 'Veuillez vérifier nos comptes', 'manager1_id', '2022-12-15T08:00:00.00Z', 'MANAGER');


insert into announcement_target(id, announcement_id, group_id)
values ('announcement_target1_id1', 'announcement3_id', 'group1_id');