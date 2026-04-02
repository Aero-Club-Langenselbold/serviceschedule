package de.ct.manager.schedule.alarm;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class StringToAlarmStatusConverter implements Converter<String, AlarmStatus> {

    @Override
    public AlarmStatus convert(@NonNull String source) {
        try {
            return AlarmStatus.fromCode(Integer.parseInt(source.trim()));
        } catch (NumberFormatException e) {
            return AlarmStatus.valueOf(source.trim().toUpperCase());
        }
    }
}
