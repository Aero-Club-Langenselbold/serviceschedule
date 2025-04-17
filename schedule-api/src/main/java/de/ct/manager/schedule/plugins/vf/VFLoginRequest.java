package de.ct.manager.schedule.plugins.vf;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class VFLoginRequest extends VFRequest {
    private String appkey;
    private String username;
    private String password;
}
