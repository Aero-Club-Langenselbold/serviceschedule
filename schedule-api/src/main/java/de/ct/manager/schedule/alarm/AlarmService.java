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
    private final AlarmDeviceStatusRepository alarmDeviceStatusRepository;

    public AlarmService(AlarmRepository alarmRepository, AlarmDeviceStatusRepository alarmDeviceStatusRepository) {
        this.alarmRepository = alarmRepository;
        this.alarmDeviceStatusRepository = alarmDeviceStatusRepository;
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

    public AlarmDeviceStatus saveDeviceStatus(AlarmDeviceStatusRequest request) {
        if (alarmDeviceStatusRepository.existsByTimestamp(request.getTimestamp())) {
            throw new AlarmDuplicateException(request.getTimestamp());
        }

        AlarmDeviceStatus status = new AlarmDeviceStatus();
        status.setTimestamp(request.getTimestamp());
        status.setBetrieb(request.getBetrieb());
        status.setScharf(request.getScharf());
        status.setUnscharf(request.getUnscharf());
        status.setSabotage(request.getSabotage());
        status.setStoerung(request.getStoerung());
        status.setAlarm(request.getAlarm());
        status.setZone1(request.getZone1());
        status.setZone2(request.getZone2());
        status.setZone3(request.getZone3());
        status.setZone4(request.getZone4());
        status.setZone5(request.getZone5());
        status.setZone6(request.getZone6());
        status.setZone7(request.getZone7());
        status.setZone8(request.getZone8());

        return alarmDeviceStatusRepository.save(status);
    }

    public Page<AlarmDeviceStatus> getDeviceStatuses(Pageable pageable) {
        return alarmDeviceStatusRepository.findAll(pageable);
    }
}
