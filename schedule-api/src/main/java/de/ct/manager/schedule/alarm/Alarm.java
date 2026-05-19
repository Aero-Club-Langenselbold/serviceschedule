package de.ct.manager.schedule.alarm;

import java.time.LocalDateTime;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "alarm")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Alarm extends AbstractPersistentEntity {

    @Column(name = "alarm_timestamp", nullable = false, unique = true)
    private LocalDateTime timestamp;

    @Column(name = "system_timestamp")
    private LocalDateTime systemTimestamp;

    @Column(name = "rfid_id", length = 32)
    private String rfidId;

    @Column(name = "internal_time", length = 10)
    private String internalTime;

    @Column(name = "raw_data", length = 255)
    private String rawData;

    @Enumerated(EnumType.ORDINAL)
    @Column(columnDefinition = "INTEGER")
    private AlarmStatus status;

    @Column
    private boolean armed;

    @Column
    private boolean disarmed;

    @Column
    private boolean sabotage;

    @Column
    private boolean fault;

    @Column
    private boolean alarm;

    @Column
    private boolean zone1;

    @Column
    private boolean zone2;

    @Column
    private boolean zone3;

    @Column
    private boolean zone4;

    @Column
    private boolean zone5;

    @Column
    private boolean zone6;

    @Column
    private boolean zone7;

    @Column
    private boolean zone8;
}
