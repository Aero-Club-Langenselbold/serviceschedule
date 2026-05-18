package de.ct.manager.schedule.alarm;

import java.time.LocalDateTime;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alarm_device_status")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AlarmDeviceStatus extends AbstractPersistentEntity {

    @Column(name = "alarm_timestamp", nullable = false, unique = true)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Integer betrieb;

    @Column(nullable = false)
    private Integer scharf;

    @Column(nullable = false)
    private Integer unscharf;

    @Column(nullable = false)
    private Integer sabotage;

    @Column(nullable = false)
    private Integer stoerung;

    @Column(nullable = false)
    private Integer alarm;

    @Column(nullable = false)
    private Integer zone1;

    @Column(nullable = false)
    private Integer zone2;

    @Column(nullable = false)
    private Integer zone3;

    @Column(nullable = false)
    private Integer zone4;

    @Column(nullable = false)
    private Integer zone5;

    @Column(nullable = false)
    private Integer zone6;

    @Column(nullable = false)
    private Integer zone7;

    @Column(nullable = false)
    private Integer zone8;
}
