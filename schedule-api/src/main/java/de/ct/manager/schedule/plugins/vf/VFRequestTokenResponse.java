package de.ct.manager.schedule.plugins.vf;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class VFRequestTokenResponse extends VFResponse {
    private String accesstoken;
}
