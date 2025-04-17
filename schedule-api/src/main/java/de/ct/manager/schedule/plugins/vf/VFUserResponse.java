package de.ct.manager.schedule.plugins.vf;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class VFUserResponse extends VFResponse{
    private String uid;
    private String firstname;
    private String lastname;
    private String email;
    private String memberid;
    private String status;
    private String cid;
}
