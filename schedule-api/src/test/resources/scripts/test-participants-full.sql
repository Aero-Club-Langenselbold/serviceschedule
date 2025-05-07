INSERT INTO service (id, version, servicename, add_at_import) VALUES
    ('5111', 0, 'Funky Service', false),
    ('5112', 0, 'Cool Service', false),
    ('5113', 0, 'Awesome Service', false),
    ('5114', 0, 'Super Service', false),
    ('5115', 0, 'Mega Service', false);

INSERT INTO participant (id, version, firstname, lastname, email, status, date_of_birth, max_assignments) VALUES
    ('4711', 0, 'Donald', 'Duck', 'donald@angermanagementissues.com', 'ACTIVE', '1934-06-09', 0),
    ('4712', 0, 'Mickey', 'Mouse', 'mickey@funwithtests.com', 'ACTIVE', '1928-11-18', 0),
    ('4713', 0, 'Minnie', 'Mouse', 'minnie@funwithtests.com', 'ACTIVE', '1928-11-18', 0),
    ('4714', 0, 'Goofy', 'Goof', 'goofy@funwithtests.com', 'ACTIVE', '1932-05-25', 0),
    ('4715', 0, 'Daisy', 'Duck', 'daisy@funwithtests.com', 'ACTIVE', '1940-01-01', 0),
    ('4716', 0, 'Pluto', 'Dog', 'pluto@funwithtests.com', 'ACTIVE', '1930-09-05', 0),
    ('4717', 0, 'Scrooge', 'McDuck', 'scrooge@funwithtests.com', 'ACTIVE', '1947-12-22', 0),
    ('4718', 0, 'Huey', 'Duck', 'huey@funwithtests.com', 'ACTIVE', '1937-10-17', 0),
    ('4719', 0, 'Dewey', 'Duck', 'dewey@funwithtests.com', 'ACTIVE', '1937-10-17', 0),
    ('4720', 0, 'Louie', 'Duck', 'louie@funwithtests.com', 'ACTIVE', '1937-10-17', 0),
    ('4721', 0, 'Chip', 'Chipmunk', 'chip@funwithtests.com', 'ACTIVE', '1943-04-02', 0),
    ('4722', 0, 'Dale', 'Chipmunk', 'dale@funwithtests.com', 'ACTIVE', '1943-04-02', 0),
    ('4723', 0, 'Clarice', 'Chipmunk', 'clarice@funwithtests.com', 'ACTIVE', '1952-06-07', 0),
    ('4724', 0, 'Max', 'Goof', 'max@funwithtests.com', 'ACTIVE', '1992-09-05', 0),
    ('4725', 0, 'Pete', 'Cat', 'pete@funwithtests.com', 'ACTIVE', '1925-11-18', 0),
    ('4726', 0, 'Horace', 'Horsecollar', 'horace@funwithtests.com', 'ACTIVE', '1929-11-18', 0),
    ('4727', 0, 'Clarabelle', 'Cow', 'clarabelle@funwithtests.com', 'ACTIVE', '1928-11-18', 0),
    ('4728', 0, 'José', 'Carioca', 'jose@funwithtests.com', 'ACTIVE', '1942-08-24', 0),
    ('4729', 0, 'Panchito', 'Pistoles', 'panchito@funwithtests.com', 'ACTIVE', '1944-12-21', 0),
    ('4730', 0, 'Ludwig', 'Von Drake', 'ludwig@funwithtests.com', 'ACTIVE', '1961-09-24', 0),
    ('4731', 0, 'Oswald', 'Rabbit', 'oswald@funwithtests.com', 'ACTIVE', '1927-09-05', 0);

INSERT INTO participant_service (participant_id, service_id, auto_assign) VALUES
    ('4711', '5111', true), ('4711', '5112', true), ('4711', '5113', true), ('4711', '5114', true), ('4711', '5115', true),
    ('4712', '5111', true), ('4712', '5112', true), ('4712', '5113', true), ('4712', '5114', true), ('4712', '5115', true),
    ('4713', '5111', true), ('4713', '5112', true), ('4713', '5113', true), ('4713', '5114', true), ('4713', '5115', true),
    ('4714', '5111', true), ('4714', '5112', true), ('4714', '5113', true), ('4714', '5114', true), ('4714', '5115', true),
    ('4715', '5111', true), ('4715', '5112', true), ('4715', '5113', true), ('4715', '5114', true), ('4715', '5115', true),
    ('4716', '5111', true), ('4716', '5112', true), ('4716', '5113', true), ('4716', '5114', true), ('4716', '5115', true),
    ('4717', '5111', true), ('4717', '5112', true), ('4717', '5113', true), ('4717', '5114', true), ('4717', '5115', true),
    ('4718', '5111', true), ('4718', '5112', true), ('4718', '5113', true), ('4718', '5114', true), ('4718', '5115', true),
    ('4719', '5111', true), ('4719', '5112', true), ('4719', '5113', true), ('4719', '5114', true), ('4719', '5115', true),
    ('4720', '5111', true), ('4720', '5112', true), ('4720', '5113', true), ('4720', '5114', true), ('4720', '5115', true),
    ('4721', '5111', true), ('4721', '5112', true), ('4721', '5113', true), ('4721', '5114', true), ('4721', '5115', true),
    ('4722', '5111', true), ('4722', '5112', true), ('4722', '5113', true), ('4722', '5114', true), ('4722', '5115', true),
    ('4723', '5111', true), ('4723', '5112', true), ('4723', '5113', true), ('4723', '5114', true), ('4723', '5115', true),
    ('4724', '5111', true), ('4724', '5112', true), ('4724', '5113', true), ('4724', '5114', true), ('4724', '5115', true),
    ('4725', '5111', true), ('4725', '5112', true), ('4725', '5113', true), ('4725', '5114', true), ('4725', '5115', true),
    ('4726', '5111', true), ('4726', '5112', true), ('4726', '5113', true), ('4726', '5114', true), ('4726', '5115', true),
    ('4727', '5111', true), ('4727', '5112', true), ('4727', '5113', true), ('4727', '5114', true), ('4727', '5115', true),
    ('4728', '5111', true), ('4728', '5112', true), ('4728', '5113', true), ('4728', '5114', true), ('4728', '5115', true),
    ('4729', '5111', true), ('4729', '5112', true), ('4729', '5113', true), ('4729', '5114', true), ('4729', '5115', true),
    ('4730', '5111', true), ('4730', '5112', true), ('4730', '5113', true), ('4730', '5114', true), ('4730', '5115', true),
    ('4731', '5111', true), ('4731', '5112', true), ('4731', '5113', true), ('4731', '5114', true), ('4731', '5115', true);

INSERT INTO common_settings (id, version, setting_key, setting_value) VALUES ('550e8400-e29b-41d4-a716-446655440000', 0, 'min_participation_count', '4');