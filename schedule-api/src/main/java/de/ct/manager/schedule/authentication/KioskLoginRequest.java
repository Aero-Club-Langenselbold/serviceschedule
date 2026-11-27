package de.ct.manager.schedule.authentication;

import jakarta.validation.constraints.NotBlank;

/** Kiosk sign in: e-mail address and the 6 digit PIN from the personal settings. */
public record KioskLoginRequest(@NotBlank String username, @NotBlank String pin) {
}
