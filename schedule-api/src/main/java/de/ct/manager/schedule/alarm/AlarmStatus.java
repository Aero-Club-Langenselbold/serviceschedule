package de.ct.manager.schedule.alarm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AlarmStatus {
    UNAUTHORIZED_KEY(0),
    AUTHORIZED_KEY(1),
    UNKNOWN_KEY(2),
    NOT_IMPLEMENTED(3);

    private final int code;

    AlarmStatus(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static AlarmStatus fromCode(int code) {
        for (AlarmStatus status : values()) {
            if (status.code == code) return status;
        }
        throw new IllegalArgumentException("Unknown alarm status code: " + code);
    }
}
