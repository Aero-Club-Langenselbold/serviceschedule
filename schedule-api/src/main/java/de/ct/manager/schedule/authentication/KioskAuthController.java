package de.ct.manager.schedule.authentication;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth/kiosk")
public class KioskAuthController {

    private final KioskAuthService kioskAuthService;

    public KioskAuthController(KioskAuthService kioskAuthService) {
        this.kioskAuthService = kioskAuthService;
    }

    /** Opens a kiosk session (plain member, maintenance and operations only); 401 for a wrong or missing PIN, 429 when locked. */
    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> signIn(@Valid @RequestBody KioskLoginRequest request) {
        return ResponseEntity.ok(kioskAuthService.signIn(request));
    }
}
