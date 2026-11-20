INSERT INTO service (id, version, servicename, add_at_import) VALUES
    ('ps-s1', 0, 'Flugleiter', false),
    ('ps-s2', 0, 'Windenfahrer', false);

INSERT INTO participant (id, version, firstname, lastname, email, status, date_of_birth, max_assignments, min_assignments) VALUES
    ('ps-p1', 0, 'Tina', 'Test', 'test@test.com', 'ACTIVE', '1990-01-01', 0, 0),
    ('ps-p2', 0, 'Kurt', 'Kollege', 'kurt@example.org', 'ACTIVE', '1980-01-01', 0, 0);

INSERT INTO plan (id, version, name, description, date_from, date_until, published) VALUES
    ('ps-old', 0, 'Saison 2020', NULL, '2020-03-01 00:00:00', '2020-10-31 00:00:00', true),
    ('ps-new', 0, 'Saison 2099', NULL, '2099-03-01 00:00:00', '2099-10-31 00:00:00', true),
    ('ps-draft', 0, 'Entwurf', NULL, '2099-03-01 00:00:00', '2099-10-31 00:00:00', false);

INSERT INTO plan_entry (id, version, start_time, end_time, service_id, participant_id, plan_id) VALUES
    ('ps-e1', 0, '2020-05-02 09:00:00', '2020-05-02 18:00:00', 'ps-s1', 'ps-p1', 'ps-old'),
    ('ps-e2', 0, '2099-05-03 09:00:00', '2099-05-03 18:00:00', 'ps-s2', 'ps-p1', 'ps-new'),
    ('ps-e3', 0, '2099-04-12 09:00:00', '2099-04-12 13:30:00', 'ps-s1', 'ps-p1', 'ps-new'),
    ('ps-e4', 0, '2099-04-12 09:00:00', '2099-04-12 13:30:00', 'ps-s1', 'ps-p2', 'ps-new'),
    ('ps-e5', 0, '2099-04-12 09:00:00', '2099-04-12 13:30:00', 'ps-s2', 'ps-p2', 'ps-new'),
    ('ps-e6', 0, '2099-06-01 09:00:00', '2099-06-01 18:00:00', 'ps-s1', 'ps-p1', 'ps-draft');
