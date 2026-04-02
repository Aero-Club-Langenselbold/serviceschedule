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
import de.ct.manager.schedule.plugins.FeatureConstants;
import de.ct.manager.schedule.plugins.vf.VFConnector;
import de.ct.manager.schedule.security.JwtUtils;
import de.ct.manager.schedule.settings.SettingConstants;
import de.ct.manager.schedule.settings.SettingsService;
import de.ct.manager.schedule.user.Role;
import de.ct.manager.schedule.user.RoleRepository;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import de.ct.manager.schedule.user.UserRepository;
import de.ct.manager.schedule.user.UserRole;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ParticipantRepository participantRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final FF4j ff4j;
    private final VFConnector vfConnector;
    private final SettingsService settingsService;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository, RoleRepository roleRepository, ParticipantRepository participantRepository, PasswordEncoder encoder, JwtUtils jwtUtils, FF4j ff4j, VFConnector vfConnector, SettingsService settingsService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.participantRepository = participantRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.ff4j = ff4j;
        this.vfConnector = vfConnector;
        this.settingsService = settingsService;
    }

    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication;

        if (this.ff4j.check(FeatureConstants.SECURITY_QUESTION_FEATURE)) {
            String correctAnswer = settingsService.getCommonSettingValue(SettingConstants.SECURITY_ANSWER);
            if (correctAnswer == null || !correctAnswer.toLowerCase().equals(loginRequest.getSecurityAnswer().toLowerCase())) {
                return ResponseEntity
                    .badRequest()
                    .build();
            }
        }

        if (this.ff4j.check(FeatureConstants.VF_LOGIN_FEATURE)) {
            authentication = vfConnector.signIn(loginRequest);
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
        Role userRole = roleRepository.findByName(UserRole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role not found."));

        User user = new User(signUpRequest.getUsername(),
                        encoder.encode(signUpRequest.getPassword()));
        user.getRoles().add(userRole);

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}
