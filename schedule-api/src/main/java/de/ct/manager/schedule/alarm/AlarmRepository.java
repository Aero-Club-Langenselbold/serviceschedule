package de.ct.manager.schedule.alarm;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AlarmRepository extends JpaRepository<Alarm, String>, JpaSpecificationExecutor<Alarm> {

    boolean existsByTimestamp(LocalDateTime timestamp);
}
