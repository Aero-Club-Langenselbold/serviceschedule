package de.ct.manager.schedule.user;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOverview {
    private String id;
    private String username;
    private String fullname;
    private List<String> roles;
    private List<String> permissions;
}
