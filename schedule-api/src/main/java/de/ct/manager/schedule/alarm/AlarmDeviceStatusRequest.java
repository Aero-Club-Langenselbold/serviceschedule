package de.ct.manager.schedule.alarm;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlarmDeviceStatusRequest {

    @NotNull
    private LocalDateTime timestamp;

    @NotNull
    private Integer betrieb;

    @NotNull
    private Integer scharf;

    @NotNull
    private Integer unscharf;

    @NotNull
    private Integer sabotage;

    @NotNull
    private Integer stoerung;

    @NotNull
    private Integer alarm;

    @NotNull
    private Integer zone1;

    @NotNull
    private Integer zone2;

    @NotNull
    private Integer zone3;

    @NotNull
    private Integer zone4;

    @NotNull
    private Integer zone5;

    @NotNull
    private Integer zone6;

    @NotNull
    private Integer zone7;

    @NotNull
    private Integer zone8;
}
