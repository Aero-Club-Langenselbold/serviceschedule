package de.ct.manager.schedule.alarm;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/alarm")
@PreAuthorize("hasAnyRole('ALARM_ADMIN', 'ADMIN')")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @PostMapping("/save")
    public ResponseEntity<Alarm> addAlarm(@Valid @RequestBody AlarmRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alarmService.addAlarm(request));
    }

    @GetMapping("/page")
    public ResponseEntity<Page<Alarm>> getAlarms(
            @RequestParam(required = false) String rfidId,
            @RequestParam(required = false) AlarmStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        return ResponseEntity.ok(alarmService.getAlarms(rfidId, status, from, to, pageable));
    }
}
