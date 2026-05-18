package de.ct.manager.schedule.alarm;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmDeviceStatusRepository extends JpaRepository<AlarmDeviceStatus, String> {

    boolean existsByTimestamp(LocalDateTime timestamp);
}
