INSERT INTO participant (id, version, firstname, lastname, email, status, date_of_birth, max_assignments) VALUES
    ('4711', 0, 'Donald', 'Duck', 'donald@angermanagementissues.com', 'ACTIVE', '1934-06-09', 0),
    ('4712', 0, 'Mickey', 'Mouse', 'mickey@funwithtests.com', 'ACTIVE', '1928-11-18', 0),
    ('4713', 0, 'Minnie', 'Mouse', 'minnie@funwithtests.com', 'ACTIVE', '1928-11-18', 0),
    ('4714', 0, 'Goofy', 'Goof', 'goofy@funwithtests.com', 'ACTIVE', '1932-05-25', 0),
    ('4715', 0, 'Daisy', 'Duck', 'daisy@funwithtests.com', 'ACTIVE', '1940-01-01', 0),
    ('4716', 0, 'Pluto', 'Dog', 'pluto@funwithtests.com', 'ACTIVE', '1930-09-05', 0),
    ('4717', 0, 'Scrooge', 'McDuck', 'scrooge@funwithtests.com', 'ACTIVE', '1947-12-22', 0),
    ('4718', 0, 'Huey', 'Duck', 'huey@funwithtests.com', 'ACTIVE', '1937-10-17', 0),
    ('4719', 0, 'Dewey', 'Duck', 'dewey@funwithtests.com', 'ACTIVE', '1937-10-17', 0);

INSERT INTO service (id, version, servicename, add_at_import) VALUES
    ('5111', 0, 'Funky Service', false),
    ('5112', 0, 'Cool Service', false),
    ('5113', 0, 'Awesome Service', false),
    ('5114', 0, 'Super Service', false),
    ('5115', 0, 'Mega Service', false);

INSERT INTO plan (id, version, name, description, date_from, date_until, published) VALUES
    ('6000', 0, 'World Domination tour', 'Schedule for all team members trying to take over the world', '2020-01-01 00:00:00', '2020-07-01 00:00:00', false),
    ('6001', 0, 'Marketing Blitz', 'Intensive marketing campaign', '2021-03-01 00:00:00', '2021-06-01 00:00:00', false),
    ('6002', 0, 'Product Launch', 'Launch schedule for new product', '2021-05-01 00:00:00', '2021-12-01 00:00:00', false),
    ('6003', 0, 'Team Building Retreat', 'Schedule for team building activities', '2021-07-15 00:00:00', '2021-07-20 00:00:00', false),
    ('6004', 0, 'End of Year Review', 'Review schedule for end of year performance', '2021-12-01 00:00:00', '2021-12-31 00:00:00', false),
    ('6005', 0, 'Q1 Sales Push', 'Sales push for the first quarter', '2022-01-01 00:00:00', '2022-03-31 00:00:00', false),
    ('6006', 0, 'Annual General Meeting', 'Schedule for the annual general meeting', '2022-04-01 00:00:00', '2022-04-02 00:00:00', false),
    ('6007', 0, 'Summer Internship Program', 'Schedule for summer interns', '2022-06-01 00:00:00', '2022-08-31 00:00:00', false),
    ('6008', 0, 'Holiday Season Campaign', 'Marketing campaign for the holiday season', '2022-11-01 00:00:00', '2022-12-31 00:00:00', false),
    ('6009', 0, 'New Year Kickoff', 'Kickoff schedule for the new year', '2023-01-01 00:00:00', '2023-01-15 00:00:00', false);

INSERT INTO plan_entry (id, version, start_time, end_time, service_id, participant_id, plan_id) VALUES
    ('7500', 0, '2020-01-01 10:00:00', '2020-01-01 18:00:00', '5111', '4711', '6000'),
    ('7501', 0, '2020-01-02 09:00:00', '2020-01-02 17:00:00', '5112', '4712', '6000'),
    ('7502', 0, '2020-01-03 08:00:00', '2020-01-03 16:00:00', '5113', '4713', '6000'),
    ('7503', 0, '2021-03-01 09:00:00', '2021-03-01 17:00:00', '5114', '4714', '6001'),
    ('7504', 0, '2021-03-02 10:00:00', '2021-03-02 18:00:00', '5115', '4715', '6001'),
    ('7505', 0, '2021-05-01 09:00:00', '2021-05-01 17:00:00', '5111', '4716', '6002'),
    ('7506', 0, '2021-05-02 08:00:00', '2021-05-02 16:00:00', '5112', '4717', '6002'),
    ('7507', 0, '2021-07-15 09:00:00', '2021-07-15 17:00:00', '5113', '4718', '6003'),
    ('7508', 0, '2021-07-16 10:00:00', '2021-07-16 18:00:00', '5114', '4719', '6003'),
    ('7509', 0, '2021-12-01 09:00:00', '2021-12-01 17:00:00', '5115', '4711', '6004'),
    ('7510', 0, '2021-12-02 08:00:00', '2021-12-02 16:00:00', '5111', '4712', '6004'),
    ('7511', 0, '2022-01-01 09:00:00', '2022-01-01 17:00:00', '5112', '4713', '6005'),
    ('7512', 0, '2022-01-02 10:00:00', '2022-01-02 18:00:00', '5113', '4714', '6005'),
    ('7513', 0, '2022-04-01 09:00:00', '2022-04-01 17:00:00', '5114', '4715', '6006'),
    ('7514', 0, '2022-04-02 08:00:00', '2022-04-02 16:00:00', '5115', '4716', '6006'),
    ('7515', 0, '2022-06-01 09:00:00', '2022-06-01 17:00:00', '5111', '4717', '6007'),
    ('7516', 0, '2022-06-02 10:00:00', '2022-06-02 18:00:00', '5112', '4718', '6007'),
    ('7517', 0, '2022-11-01 09:00:00', '2022-11-01 17:00:00', '5113', '4719', '6008'),
    ('7518', 0, '2022-11-02 08:00:00', '2022-11-02 16:00:00', '5114', '4711', '6008'),
    ('7519', 0, '2023-01-01 09:00:00', '2023-01-01 17:00:00', '5115', '4712', '6009');