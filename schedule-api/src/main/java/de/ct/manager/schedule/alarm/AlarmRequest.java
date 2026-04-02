package de.ct.manager.schedule.alarm;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlarmRequest {

    @NotNull
    private LocalDateTime timestamp;

    @Size(max = 32)
    private String rfidId;

    @Size(max = 10)
    private String internalTime;

    @Size(max = 255)
    private String rawData;

    @NotNull
    private AlarmStatus status;
}
