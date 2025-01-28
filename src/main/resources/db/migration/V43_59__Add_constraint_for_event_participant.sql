alter table "event_participant" add constraint unique_event_participant unique (event_id, participant_id);
