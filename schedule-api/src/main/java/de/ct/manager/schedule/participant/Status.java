package de.ct.manager.schedule.participant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Status {
    ACTIVE, INACTIVE;

    @JsonValue
    public String toValue() {
        return name();
    }
}
