package de.ct.manager.schedule.alarm;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        alarm.setTimestamp(LocalDateTime.now(ZoneOffset.UTC));
        alarm.setSystemTimestamp(request.getTimestamp());
        alarm.setRfidId(request.getRfidId());
        alarm.setInternalTime(request.getInternalTime());
        alarm.setRawData(request.getRawData());
        alarm.setStatus(request.getStatus());

        return alarmRepository.save(alarm);
    }

    public Page<Alarm> getAlarms(String rfidId, AlarmStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Page<Alarm> page = alarmRepository.findAll(buildSpec(rfidId, status, from, to), pageable);
        page.getContent().forEach(alarm -> alarm.setRfidId(anonymizeRfid(alarm.getRfidId())));
        return page;
    }

    private String anonymizeRfid(String rfid) {
        if (rfid == null || rfid.length() <= 5) {
            return rfid;
        }
        return "X".repeat(rfid.length() - 5) + rfid.substring(rfid.length() - 5);
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

    public Alarm saveDeviceStatus(AlarmDeviceStatusRequest request) {
        if (alarmRepository.existsByTimestamp(request.getTimestamp())) {
            throw new AlarmDuplicateException(request.getTimestamp());
        }

        Alarm status = new Alarm();
        status.setTimestamp(LocalDateTime.now(ZoneOffset.UTC));
        status.setSystemTimestamp(request.getTimestamp());
        status.setStatus(AlarmStatus.fromCode(request.getBetrieb()));
        status.setArmed(request.getScharf() == 1);
        status.setDisarmed(request.getUnscharf() == 1);
        status.setSabotage(request.getSabotage() == 1);
        status.setFault(request.getStoerung() == 1);
        status.setAlarm(request.getAlarm() == 1);
        status.setZone1(request.getZone1() == 1);
        status.setZone2(request.getZone2() == 1);
        status.setZone3(request.getZone3() == 1);
        status.setZone4(request.getZone4() == 1);
        status.setZone5(request.getZone5() == 1);
        status.setZone6(request.getZone6() == 1);
        status.setZone7(request.getZone7() == 1);
        status.setZone8(request.getZone8() == 1);

        return alarmRepository.save(status);
    }
}
