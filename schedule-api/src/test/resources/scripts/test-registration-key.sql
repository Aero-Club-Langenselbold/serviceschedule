DELETE FROM auth_user;

INSERT INTO participant (id, version, firstname, lastname, email, status, date_of_birth, registration_key, max_assignments, min_assignments) VALUES
    ('4711', 0, 'Donald', 'Duck', 'test@test.com', 'ACTIVE', '1934-06-09', '4711', 0, 0),
    ('4712', 0, 'Mickey', 'Mouse', 'admin@test.de', 'ACTIVE', '1928-11-18', '4711', 0, 0);