package de.ct.manager.schedule.authentication;

import java.util.List;

import de.ct.manager.schedule.user.UserDetailsImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private static final String PERMISSION_PREFIX = "PERMISSION_";

    private String token;
    private String type = "Bearer";
    private String id;
    private String username;
    private List<String> roles;
    private List<String> permissions;
    private String impersonator;

    public static JwtResponse of(String token, UserDetailsImpl userDetails, String impersonator) {
        List<String> authorities = userDetails.getAuthorities().stream().map(item -> item.getAuthority()).toList();

        return new JwtResponse(token, "Bearer",
                                userDetails.getId(),
                                userDetails.getUsername(),
                                authorities.stream().filter(authority -> !authority.startsWith(PERMISSION_PREFIX)).toList(),
                                authorities.stream().filter(authority -> authority.startsWith(PERMISSION_PREFIX)).toList(),
                                impersonator);
    }
}
