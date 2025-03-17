-- Create table for countries
CREATE TABLE country (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    name character varying(100) NOT NULL
);

ALTER TABLE country ADD CONSTRAINT country_pkey PRIMARY KEY (id);

INSERT INTO country (id, version, name) VALUES ('f47ac10b-58cc-4372-a567-0e02b2c3d479', 0, 'Germany');

-- Create table for states
CREATE TABLE state (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    name character varying(100) NOT NULL,
    country_id character varying(36) NOT NULL
);

ALTER TABLE state ADD CONSTRAINT state_pkey PRIMARY KEY (id);
ALTER TABLE state ADD CONSTRAINT fk_state_country FOREIGN KEY (country_id) REFERENCES country(id);

INSERT INTO state (id, version, name, country_id) VALUES ('1b4e28ba-2fa1-11d2-883f-0016d3cca427', 0, 'Baden-Württemberg', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('2c4e28ba-3fa1-11d2-883f-0016d3cca428', 0, 'Bavaria', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('3d4e28ba-4fa1-11d2-883f-0016d3cca429', 0, 'Berlin', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('4e4e28ba-5fa1-11d2-883f-0016d3cca430', 0, 'Brandenburg', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('5f4e28ba-6fa1-11d2-883f-0016d3cca431', 0, 'Bremen', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('6a4e28ba-7fa1-11d2-883f-0016d3cca432', 0, 'Hamburg', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('7b4e28ba-8fa1-11d2-883f-0016d3cca433', 0, 'Hesse', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('8c4e28ba-9fa1-11d2-883f-0016d3cca434', 0, 'Lower Saxony', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('9d4e28ba-afa1-11d2-883f-0016d3cca435', 0, 'Mecklenburg-Vorpommern', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('ae4e28ba-bfa1-11d2-883f-0016d3cca436', 0, 'North Rhine-Westphalia', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('bf4e28ba-cfa1-11d2-883f-0016d3cca437', 0, 'Rhineland-Palatinate', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('cf4e28ba-dfa1-11d2-883f-0016d3cca438', 0, 'Saarland', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('df4e28ba-efa1-11d2-883f-0016d3cca439', 0, 'Saxony', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('ef4e28ba-ffa1-11d2-883f-0016d3cca440', 0, 'Saxony-Anhalt', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('ff4e28ba-0fa2-11d3-883f-0016d3cca441', 0, 'Schleswig-Holstein', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');
INSERT INTO state (id, version, name, country_id) VALUES ('0f4e28ba-1fa2-11d3-883f-0016d3cca442', 0, 'Thuringia', 'f47ac10b-58cc-4372-a567-0e02b2c3d479');

-- Create table for holidays
CREATE TABLE holiday (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    name character varying(100) NOT NULL,
    date character varying(5),
    formula character varying(20)
);

ALTER TABLE holiday ADD CONSTRAINT holiday_pkey PRIMARY KEY (id);

-- Insert German holidays
INSERT INTO holiday (id, version, name, date, formula) VALUES ('1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d', 0, 'New Years Day', '01-01', null);
INSERT INTO holiday (id, version, name, date, formula) VALUES ('a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 0, '3 Kings', '01-06', null); -- 6th of January
INSERT INTO holiday (id, version, name, date, formula) VALUES ('1d2e3f4a-5b6c-7d8e-9f0a-1b2c3d4e5f6a', 0, 'Womans Day', '03-08', null);
INSERT INTO holiday (id, version, name, date, formula) VALUES ('2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e', 0, 'Good Friday', null, 'EASTER_FRIDAY');
INSERT INTO holiday (id, version, name, date, formula) VALUES ('3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f', 0, 'Easter Monday', null, 'EASTER_MONDAY');
INSERT INTO holiday (id, version, name, date, formula) VALUES ('4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a', 0, 'Labour Day', '05-01', null);
INSERT INTO holiday (id, version, name, date, formula) VALUES ('5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b', 0, 'Ascension Day', null, 'ASCENSION_DAY'); -- 39 days after Easter Sunday
INSERT INTO holiday (id, version, name, date, formula) VALUES ('6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c', 0, 'Whit Monday', null, 'WHIT_MONDAY'); -- 50 days after Easter Sunday
INSERT INTO holiday (id, version, name, date, formula) VALUES ('7f8e9d0c-1b2a-3c4d-5e6f-7a8b9c0d1e2f', 0, 'Corpus Christi', null, 'CORPUS_CHRISTI'); -- 60 days after Easter Sunday
INSERT INTO holiday (id, version, name, date, formula) VALUES ('8d9e0f1a-2b3c-4d5e-6f7a-8b9c0d1e2f3a', 0, 'Mariä Ascension', '08-15', null); -- 15th of August
INSERT INTO holiday (id, version, name, date, formula) VALUES ('9e0f1a2b-3c4d-5e6f-7a8b-9c0d1e2f3a4b', 0, 'World childrens day', '09-20', null); -- 20th of September
INSERT INTO holiday (id, version, name, date, formula) VALUES ('7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d', 0, 'German Unity Day', '10-03', null);
INSERT INTO holiday (id, version, name, date, formula) VALUES ('6a7b8c9d-0e1f-2a3b-4c5d-6e7f8a9b0c1d', 0, 'Reformation Day', '10-31', null);
INSERT INTO holiday (id, version, name, date, formula) VALUES ('5a6b7c8d-9e0f-1a2b-3c4d-5e6f7a8b9c0d', 0, 'All Saints Day', '11-01', null);
INSERT INTO holiday (id, version, name, date, formula) VALUES ('7c8d9e0f-1a2b-3c4d-5e6f-7a8b9c0d1e2f', 0, 'Day of Repentance and Prayer', null, 'REPENTANCE_PRAYER'); -- Wednesday before November 23rd
INSERT INTO holiday (id, version, name, date, formula) VALUES ('8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e', 0, 'Christmas Day', '12-25', null);
INSERT INTO holiday (id, version, name, date, formula) VALUES ('9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f', 0, 'Boxing Day', '12-26', null);

CREATE TABLE state_holiday (
    state_id character varying(36) NOT NULL,
    holiday_id character varying(36) NOT NULL
);

ALTER TABLE state_holiday ADD CONSTRAINT state_holiday_pkey PRIMARY KEY (state_id, holiday_id);
ALTER TABLE state_holiday ADD CONSTRAINT fk_state_holiday_state FOREIGN KEY (state_id) REFERENCES state(id);
ALTER TABLE state_holiday ADD CONSTRAINT fk_state_holiday_holiday FOREIGN KEY (holiday_id) REFERENCES holiday(id);

-- Map holidays to states
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('1b4e28ba-2fa1-11d2-883f-0016d3cca427', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Baden-Württemberg - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('1b4e28ba-2fa1-11d2-883f-0016d3cca427', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d'); -- Baden-Württemberg - 3 Kings
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('1b4e28ba-2fa1-11d2-883f-0016d3cca427', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Baden-Württemberg - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('1b4e28ba-2fa1-11d2-883f-0016d3cca427', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Baden-Württemberg - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('1b4e28ba-2fa1-11d2-883f-0016d3cca427', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Baden-Württemberg - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('1b4e28ba-2fa1-11d2-883f-0016d3cca427', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Baden-Württemberg - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('1b4e28ba-2fa1-11d2-883f-0016d3cca427', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Baden-Württemberg - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('1b4e28ba-2fa1-11d2-883f-0016d3cca427', '7f8e9d0c-1b2a-3c4d-5e6f-7a8b9c0d1e2f'); -- Baden-Württemberg - Corpus Christi
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('1b4e28ba-2fa1-11d2-883f-0016d3cca427', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Baden-Württemberg - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('1b4e28ba-2fa1-11d2-883f-0016d3cca427', '5a6b7c8d-9e0f-1a2b-3c4d-5e6f7a8b9c0d'); -- Baden-Württemberg - All Saints Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('1b4e28ba-2fa1-11d2-883f-0016d3cca427', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Baden-Württemberg - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('1b4e28ba-2fa1-11d2-883f-0016d3cca427', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Baden-Württemberg - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('2c4e28ba-3fa1-11d2-883f-0016d3cca428', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Bavaria - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('2c4e28ba-3fa1-11d2-883f-0016d3cca428', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d'); -- Bavaria - 3 Kings
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('2c4e28ba-3fa1-11d2-883f-0016d3cca428', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Bavaria - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('2c4e28ba-3fa1-11d2-883f-0016d3cca428', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Bavaria - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('2c4e28ba-3fa1-11d2-883f-0016d3cca428', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Bavaria - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('2c4e28ba-3fa1-11d2-883f-0016d3cca428', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Bavaria - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('2c4e28ba-3fa1-11d2-883f-0016d3cca428', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Bavaria - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('2c4e28ba-3fa1-11d2-883f-0016d3cca428', '7f8e9d0c-1b2a-3c4d-5e6f-7a8b9c0d1e2f'); -- Bavaria - Corpus Christi
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('2c4e28ba-3fa1-11d2-883f-0016d3cca428', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Bavaria - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('2c4e28ba-3fa1-11d2-883f-0016d3cca428', '5a6b7c8d-9e0f-1a2b-3c4d-5e6f7a8b9c0d'); -- Bavaria - All Saints Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('2c4e28ba-3fa1-11d2-883f-0016d3cca428', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Bavaria - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('2c4e28ba-3fa1-11d2-883f-0016d3cca428', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Bavaria - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('3d4e28ba-4fa1-11d2-883f-0016d3cca429', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Berlin - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('3d4e28ba-4fa1-11d2-883f-0016d3cca429', '1d2e3f4a-5b6c-7d8e-9f0a-1b2c3d4e5f6a'); -- Berlin - Women's Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('3d4e28ba-4fa1-11d2-883f-0016d3cca429', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Berlin - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('3d4e28ba-4fa1-11d2-883f-0016d3cca429', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Berlin - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('3d4e28ba-4fa1-11d2-883f-0016d3cca429', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Berlin - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('3d4e28ba-4fa1-11d2-883f-0016d3cca429', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Berlin - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('3d4e28ba-4fa1-11d2-883f-0016d3cca429', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Berlin - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('3d4e28ba-4fa1-11d2-883f-0016d3cca429', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Berlin - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('3d4e28ba-4fa1-11d2-883f-0016d3cca429', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Berlin - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('3d4e28ba-4fa1-11d2-883f-0016d3cca429', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Berlin - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('4e4e28ba-5fa1-11d2-883f-0016d3cca430', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Brandenburg - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('4e4e28ba-5fa1-11d2-883f-0016d3cca430', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Brandenburg - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('4e4e28ba-5fa1-11d2-883f-0016d3cca430', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Brandenburg - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('4e4e28ba-5fa1-11d2-883f-0016d3cca430', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Brandenburg - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('4e4e28ba-5fa1-11d2-883f-0016d3cca430', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Brandenburg - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('4e4e28ba-5fa1-11d2-883f-0016d3cca430', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Brandenburg - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('4e4e28ba-5fa1-11d2-883f-0016d3cca430', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Brandenburg - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('4e4e28ba-5fa1-11d2-883f-0016d3cca430', '6a7b8c9d-0e1f-2a3b-4c5d-6e7f8a9b0c1d'); -- Brandenburg - Reformation Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('4e4e28ba-5fa1-11d2-883f-0016d3cca430', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Brandenburg - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('4e4e28ba-5fa1-11d2-883f-0016d3cca430', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Brandenburg - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('5f4e28ba-6fa1-11d2-883f-0016d3cca431', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Bremen - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('5f4e28ba-6fa1-11d2-883f-0016d3cca431', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Bremen - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('5f4e28ba-6fa1-11d2-883f-0016d3cca431', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Bremen - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('5f4e28ba-6fa1-11d2-883f-0016d3cca431', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Bremen - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('5f4e28ba-6fa1-11d2-883f-0016d3cca431', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Bremen - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('5f4e28ba-6fa1-11d2-883f-0016d3cca431', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Bremen - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('5f4e28ba-6fa1-11d2-883f-0016d3cca431', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Bremen - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('5f4e28ba-6fa1-11d2-883f-0016d3cca431', '6a7b8c9d-0e1f-2a3b-4c5d-6e7f8a9b0c1d'); -- Bremen - Reformation Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('5f4e28ba-6fa1-11d2-883f-0016d3cca431', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Bremen - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('5f4e28ba-6fa1-11d2-883f-0016d3cca431', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Bremen - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('6a4e28ba-7fa1-11d2-883f-0016d3cca432', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Hamburg - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('6a4e28ba-7fa1-11d2-883f-0016d3cca432', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Hamburg - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('6a4e28ba-7fa1-11d2-883f-0016d3cca432', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Hamburg - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('6a4e28ba-7fa1-11d2-883f-0016d3cca432', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Hamburg - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('6a4e28ba-7fa1-11d2-883f-0016d3cca432', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Hamburg - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('6a4e28ba-7fa1-11d2-883f-0016d3cca432', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Hamburg - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('6a4e28ba-7fa1-11d2-883f-0016d3cca432', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Hamburg - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('6a4e28ba-7fa1-11d2-883f-0016d3cca432', '6a7b8c9d-0e1f-2a3b-4c5d-6e7f8a9b0c1d'); -- Hamburg - Reformation Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('6a4e28ba-7fa1-11d2-883f-0016d3cca432', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Hamburg - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('6a4e28ba-7fa1-11d2-883f-0016d3cca432', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Hamburg - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('7b4e28ba-8fa1-11d2-883f-0016d3cca433', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Hesse - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('7b4e28ba-8fa1-11d2-883f-0016d3cca433', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Hesse - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('7b4e28ba-8fa1-11d2-883f-0016d3cca433', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Hesse - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('7b4e28ba-8fa1-11d2-883f-0016d3cca433', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Hesse - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('7b4e28ba-8fa1-11d2-883f-0016d3cca433', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Hesse - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('7b4e28ba-8fa1-11d2-883f-0016d3cca433', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Hesse - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('7b4e28ba-8fa1-11d2-883f-0016d3cca433', '7f8e9d0c-1b2a-3c4d-5e6f-7a8b9c0d1e2f'); -- Hesse - Corpus Christi
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('7b4e28ba-8fa1-11d2-883f-0016d3cca433', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Hesse - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('7b4e28ba-8fa1-11d2-883f-0016d3cca433', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Hesse - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('7b4e28ba-8fa1-11d2-883f-0016d3cca433', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Hesse - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('8c4e28ba-9fa1-11d2-883f-0016d3cca434', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Lower Saxony - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('8c4e28ba-9fa1-11d2-883f-0016d3cca434', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Lower Saxony - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('8c4e28ba-9fa1-11d2-883f-0016d3cca434', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Lower Saxony - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('8c4e28ba-9fa1-11d2-883f-0016d3cca434', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Lower Saxony - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('8c4e28ba-9fa1-11d2-883f-0016d3cca434', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Lower Saxony - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('8c4e28ba-9fa1-11d2-883f-0016d3cca434', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Lower Saxony - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('8c4e28ba-9fa1-11d2-883f-0016d3cca434', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Lower Saxony - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('8c4e28ba-9fa1-11d2-883f-0016d3cca434', '6a7b8c9d-0e1f-2a3b-4c5d-6e7f8a9b0c1d'); -- Lower Saxony - Reformation Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('8c4e28ba-9fa1-11d2-883f-0016d3cca434', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Lower Saxony - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('8c4e28ba-9fa1-11d2-883f-0016d3cca434', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Lower Saxony - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('9d4e28ba-afa1-11d2-883f-0016d3cca435', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Mecklenburg-Vorpommern - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('9d4e28ba-afa1-11d2-883f-0016d3cca435', '1d2e3f4a-5b6c-7d8e-9f0a-1b2c3d4e5f6a'); -- Mecklenburg-Vorpommern - Women's Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('9d4e28ba-afa1-11d2-883f-0016d3cca435', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Mecklenburg-Vorpommern - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('9d4e28ba-afa1-11d2-883f-0016d3cca435', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Mecklenburg-Vorpommern - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('9d4e28ba-afa1-11d2-883f-0016d3cca435', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Mecklenburg-Vorpommern - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('9d4e28ba-afa1-11d2-883f-0016d3cca435', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Mecklenburg-Vorpommern - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('9d4e28ba-afa1-11d2-883f-0016d3cca435', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Mecklenburg-Vorpommern - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('9d4e28ba-afa1-11d2-883f-0016d3cca435', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Mecklenburg-Vorpommern - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('9d4e28ba-afa1-11d2-883f-0016d3cca435', '6a7b8c9d-0e1f-2a3b-4c5d-6e7f8a9b0c1d'); -- Mecklenburg-Vorpommern - Reformation Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('9d4e28ba-afa1-11d2-883f-0016d3cca435', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Mecklenburg-Vorpommern - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('9d4e28ba-afa1-11d2-883f-0016d3cca435', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Mecklenburg-Vorpommern - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ae4e28ba-bfa1-11d2-883f-0016d3cca436', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- North Rhine-Westphalia - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ae4e28ba-bfa1-11d2-883f-0016d3cca436', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- North Rhine-Westphalia - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ae4e28ba-bfa1-11d2-883f-0016d3cca436', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- North Rhine-Westphalia - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ae4e28ba-bfa1-11d2-883f-0016d3cca436', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- North Rhine-Westphalia - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ae4e28ba-bfa1-11d2-883f-0016d3cca436', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- North Rhine-Westphalia - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ae4e28ba-bfa1-11d2-883f-0016d3cca436', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- North Rhine-Westphalia - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ae4e28ba-bfa1-11d2-883f-0016d3cca436', '7f8e9d0c-1b2a-3c4d-5e6f-7a8b9c0d1e2f'); -- North Rhine-Westphalia - Corpus Christi
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ae4e28ba-bfa1-11d2-883f-0016d3cca436', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- North Rhine-Westphalia - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ae4e28ba-bfa1-11d2-883f-0016d3cca436', '5a6b7c8d-9e0f-1a2b-3c4d-5e6f7a8b9c0d'); -- North Rhine-Westphalia - All Saints Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ae4e28ba-bfa1-11d2-883f-0016d3cca436', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- North Rhine-Westphalia - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ae4e28ba-bfa1-11d2-883f-0016d3cca436', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- North Rhine-Westphalia - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('bf4e28ba-cfa1-11d2-883f-0016d3cca437', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Rhineland-Palatinate - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('bf4e28ba-cfa1-11d2-883f-0016d3cca437', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Rhineland-Palatinate - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('bf4e28ba-cfa1-11d2-883f-0016d3cca437', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Rhineland-Palatinate - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('bf4e28ba-cfa1-11d2-883f-0016d3cca437', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Rhineland-Palatinate - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('bf4e28ba-cfa1-11d2-883f-0016d3cca437', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Rhineland-Palatinate - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('bf4e28ba-cfa1-11d2-883f-0016d3cca437', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Rhineland-Palatinate - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('bf4e28ba-cfa1-11d2-883f-0016d3cca437', '7f8e9d0c-1b2a-3c4d-5e6f-7a8b9c0d1e2f'); -- Rhineland-Palatinate - Corpus Christi
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('bf4e28ba-cfa1-11d2-883f-0016d3cca437', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Rhineland-Palatinate - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('bf4e28ba-cfa1-11d2-883f-0016d3cca437', '5a6b7c8d-9e0f-1a2b-3c4d-5e6f7a8b9c0d'); -- Rhineland-Palatinate - All Saints Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('bf4e28ba-cfa1-11d2-883f-0016d3cca437', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Rhineland-Palatinate - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('bf4e28ba-cfa1-11d2-883f-0016d3cca437', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Rhineland-Palatinate - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('cf4e28ba-dfa1-11d2-883f-0016d3cca438', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Saarland - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('cf4e28ba-dfa1-11d2-883f-0016d3cca438', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Saarland - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('cf4e28ba-dfa1-11d2-883f-0016d3cca438', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Saarland - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('cf4e28ba-dfa1-11d2-883f-0016d3cca438', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Saarland - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('cf4e28ba-dfa1-11d2-883f-0016d3cca438', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Saarland - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('cf4e28ba-dfa1-11d2-883f-0016d3cca438', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Saarland - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('cf4e28ba-dfa1-11d2-883f-0016d3cca438', '7f8e9d0c-1b2a-3c4d-5e6f-7a8b9c0d1e2f'); -- Saarland - Corpus Christi
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('cf4e28ba-dfa1-11d2-883f-0016d3cca438', '8d9e0f1a-2b3c-4d5e-6f7a-8b9c0d1e2f3a'); -- Saarland - Mariä Himmelfahrt
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('cf4e28ba-dfa1-11d2-883f-0016d3cca438', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Saarland - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('cf4e28ba-dfa1-11d2-883f-0016d3cca438', '5a6b7c8d-9e0f-1a2b-3c4d-5e6f7a8b9c0d'); -- Saarland - All Saints Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('cf4e28ba-dfa1-11d2-883f-0016d3cca438', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Saarland - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('cf4e28ba-dfa1-11d2-883f-0016d3cca438', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Saarland - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('df4e28ba-efa1-11d2-883f-0016d3cca439', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Saxony - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('df4e28ba-efa1-11d2-883f-0016d3cca439', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Saxony - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('df4e28ba-efa1-11d2-883f-0016d3cca439', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Saxony - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('df4e28ba-efa1-11d2-883f-0016d3cca439', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Saxony - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('df4e28ba-efa1-11d2-883f-0016d3cca439', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Saxony - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('df4e28ba-efa1-11d2-883f-0016d3cca439', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Saxony - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('df4e28ba-efa1-11d2-883f-0016d3cca439', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Saxony - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('df4e28ba-efa1-11d2-883f-0016d3cca439', '6a7b8c9d-0e1f-2a3b-4c5d-6e7f8a9b0c1d'); -- Saxony - Reformation Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('df4e28ba-efa1-11d2-883f-0016d3cca439', '7c8d9e0f-1a2b-3c4d-5e6f-7a8b9c0d1e2f'); -- Saxony - Day of Repentance and Prayer
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('df4e28ba-efa1-11d2-883f-0016d3cca439', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Saxony - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('df4e28ba-efa1-11d2-883f-0016d3cca439', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Saxony - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ef4e28ba-ffa1-11d2-883f-0016d3cca440', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Saxony-Anhalt - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ef4e28ba-ffa1-11d2-883f-0016d3cca440', 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d'); -- Saxony-Anhalt - 3 Kings
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ef4e28ba-ffa1-11d2-883f-0016d3cca440', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Saxony-Anhalt - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ef4e28ba-ffa1-11d2-883f-0016d3cca440', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Saxony-Anhalt - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ef4e28ba-ffa1-11d2-883f-0016d3cca440', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Saxony-Anhalt - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ef4e28ba-ffa1-11d2-883f-0016d3cca440', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Saxony-Anhalt - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ef4e28ba-ffa1-11d2-883f-0016d3cca440', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Saxony-Anhalt - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ef4e28ba-ffa1-11d2-883f-0016d3cca440', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Saxony-Anhalt - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ef4e28ba-ffa1-11d2-883f-0016d3cca440', '6a7b8c9d-0e1f-2a3b-4c5d-6e7f8a9b0c1d'); -- Saxony-Anhalt - Reformation Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ef4e28ba-ffa1-11d2-883f-0016d3cca440', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Saxony-Anhalt - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ef4e28ba-ffa1-11d2-883f-0016d3cca440', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Saxony-Anhalt - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ff4e28ba-0fa2-11d3-883f-0016d3cca441', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Schleswig-Holstein - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ff4e28ba-0fa2-11d3-883f-0016d3cca441', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Schleswig-Holstein - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ff4e28ba-0fa2-11d3-883f-0016d3cca441', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Schleswig-Holstein - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ff4e28ba-0fa2-11d3-883f-0016d3cca441', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Schleswig-Holstein - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ff4e28ba-0fa2-11d3-883f-0016d3cca441', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Schleswig-Holstein - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ff4e28ba-0fa2-11d3-883f-0016d3cca441', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Schleswig-Holstein - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ff4e28ba-0fa2-11d3-883f-0016d3cca441', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Schleswig-Holstein - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ff4e28ba-0fa2-11d3-883f-0016d3cca441', '6a7b8c9d-0e1f-2a3b-4c5d-6e7f8a9b0c1d'); -- Schleswig-Holstein - Reformation Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ff4e28ba-0fa2-11d3-883f-0016d3cca441', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Schleswig-Holstein - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('ff4e28ba-0fa2-11d3-883f-0016d3cca441', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Schleswig-Holstein - Boxing Day

INSERT INTO state_holiday (state_id, holiday_id) VALUES ('0f4e28ba-1fa2-11d3-883f-0016d3cca442', '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d'); -- Thuringia - New Years Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('0f4e28ba-1fa2-11d3-883f-0016d3cca442', '4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a'); -- Thuringia - Labour Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('0f4e28ba-1fa2-11d3-883f-0016d3cca442', '2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e'); -- Thuringia - Good Friday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('0f4e28ba-1fa2-11d3-883f-0016d3cca442', '3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f'); -- Thuringia - Easter Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('0f4e28ba-1fa2-11d3-883f-0016d3cca442', '5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b'); -- Thuringia - Ascension Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('0f4e28ba-1fa2-11d3-883f-0016d3cca442', '6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c'); -- Thuringia - Whit Monday
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('0f4e28ba-1fa2-11d3-883f-0016d3cca442', '9e0f1a2b-3c4d-5e6f-7a8b-9c0d1e2f3a4b'); -- Thuringia - World Children's Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('0f4e28ba-1fa2-11d3-883f-0016d3cca442', '7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d'); -- Thuringia - German Unity Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('0f4e28ba-1fa2-11d3-883f-0016d3cca442', '6a7b8c9d-0e1f-2a3b-4c5d-6e7f8a9b0c1d'); -- Thuringia - Reformation Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('0f4e28ba-1fa2-11d3-883f-0016d3cca442', '8b9c0d1e-2f3a-4b5c-6d7e-8f9a0b1c2d3e'); -- Thuringia - Christmas Day
INSERT INTO state_holiday (state_id, holiday_id) VALUES ('0f4e28ba-1fa2-11d3-883f-0016d3cca442', '9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f'); -- Thuringia - Boxing Day

CREATE TABLE selected_state (
    id character varying(36) NOT NULL,
    version int4 DEFAULT 0 NOT NULL,
    country_id character varying(36) NOT NULL,
    state_id character varying(36) NOT NULL
);

ALTER TABLE selected_state ADD CONSTRAINT selected_state_pkey PRIMARY KEY (id);
ALTER TABLE selected_state ADD CONSTRAINT fk_selected_state_country FOREIGN KEY (country_id) REFERENCES country(id);
ALTER TABLE selected_state ADD CONSTRAINT fk_selected_state_state FOREIGN KEY (state_id) REFERENCES state(id);
