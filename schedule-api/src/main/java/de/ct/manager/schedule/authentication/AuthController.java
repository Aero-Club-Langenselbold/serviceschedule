package de.ct.manager.schedule.authentication;

import java.util.List;
import java.util.stream.Collectors;

import org.ff4j.FF4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.participant.ParticipantRepository;
import de.ct.manager.schedule.plugins.vf.VFConnector;
import de.ct.manager.schedule.security.JwtUtils;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.user.UserRepository;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String VF_LOGIN_FEATURE = "vfLoginFeature";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final FF4j ff4j;
    private final VFConnector vfConnector;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository, ParticipantRepository participantRepository, PasswordEncoder encoder, JwtUtils jwtUtils, FF4j ff4j, VFConnector vfConnector) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.ff4j = ff4j;
        this.vfConnector = vfConnector;
    }

    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication;

        if (this.ff4j.check(VF_LOGIN_FEATURE)) {
            authentication = authenticationManager.authenticate(vfConnector.signIn(loginRequest));
        } else {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
            .map(item -> item.getAuthority())
            .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt, "Bearer",
                                                userDetails.getId(),
                                                userDetails.getUsername(),
                                                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                .badRequest()
                .body(new MessageResponse("Error: Username is already taken!"));
        }

        Participant participant = participantRepository.findByEmail(signUpRequest.getUsername()).orElseThrow();
        if (participant.getRegistrationKey() == null || !participant.getRegistrationKey().equals(signUpRequest.getRegistrationKey())) {
            return ResponseEntity
                .badRequest()
                .body(new MessageResponse("Error: Registration key is invalid!"));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                        encoder.encode(signUpRequest.getPassword()));

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}
