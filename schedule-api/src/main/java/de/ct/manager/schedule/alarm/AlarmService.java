package de.ct.manager.schedule.alarm;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;

@Service
public class AlarmService {

    private final AlarmRepository alarmRepository;

    public AlarmService(AlarmRepository alarmRepository) {
        this.alarmRepository = alarmRepository;
    }

    public Alarm addAlarm(AlarmRequest request) {
        if (alarmRepository.existsByTimestamp(request.getTimestamp())) {
            throw new AlarmDuplicateException(request.getTimestamp());
        }

        Alarm alarm = new Alarm();
        alarm.setTimestamp(request.getTimestamp());
        alarm.setRfidId(request.getRfidId());
        alarm.setInternalTime(request.getInternalTime());
        alarm.setRawData(request.getRawData());
        alarm.setStatus(request.getStatus());

        return alarmRepository.save(alarm);
    }

    public Page<Alarm> getAlarms(String rfidId, AlarmStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return alarmRepository.findAll(buildSpec(rfidId, status, from, to), pageable);
    }

    private Specification<Alarm> buildSpec(String rfidId, AlarmStatus status, LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (rfidId != null) {
                predicates.add(cb.equal(root.get("rfidId"), rfidId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
