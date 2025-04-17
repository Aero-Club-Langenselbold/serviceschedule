package de.ct.manager.schedule.plugins.vf;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class VFLoginRequest extends VFRequest {
    private String appkey;
    private String username;
    private String password;
    @JsonProperty("auth_secret")
    private String authSecret;
}
