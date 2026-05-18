INSERT INTO alarm (id, alarm_timestamp, rfid_id, internal_time, raw_data, status, armed, disarmed, sabotage, fault, alarm, zone1, zone2, zone3, zone4, zone5, zone6, zone7, zone8) VALUES
    ('alarm-test-1', '2026-01-01 10:00:00', 'rfid001', '10:00', 'raw data 1', 0, false, false, false, false, false, false, false, false, false, false, false, false, false),
    ('alarm-test-2', '2026-01-01 11:00:00', 'rfid002', '11:00', 'raw data 2', 1, false, false, false, false, false, false, false, false, false, false, false, false, false),
    ('alarm-test-3', '2026-01-01 12:00:00', 'rfid001', '12:00', 'raw data 3', 2, false, false, false, false, false, false, false, false, false, false, false, false, false);
