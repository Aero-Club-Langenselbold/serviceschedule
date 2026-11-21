package de.ct.manager.schedule.plan;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A service on a day of a plan that has fewer people assigned than required, e.g. because nobody was available.
 */
public record UnfilledServiceDate(
    LocalDate date,
    String serviceId,
    String servicename,
    LocalTime startTime,
    LocalTime endTime,
    int required,
    int assigned
) {
}
