package de.ct.manager.schedule.alarm;

import java.time.LocalDateTime;

public class AlarmDuplicateException extends RuntimeException {

    public AlarmDuplicateException(LocalDateTime timestamp) {
        super("Alarm with timestamp " + timestamp + " already exists");
    }
}
