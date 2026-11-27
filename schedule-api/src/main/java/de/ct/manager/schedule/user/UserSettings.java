package de.ct.manager.schedule.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** The personal settings of the signed in user. The kiosk PIN itself is never returned, only whether one is set. */
public record UserSettings(String username, boolean kioskPinSet) {

    public record KioskPinRequest(@NotNull @Pattern(regexp = "\\d{6}", message = "must be 6 digits") String pin) {
    }
}
