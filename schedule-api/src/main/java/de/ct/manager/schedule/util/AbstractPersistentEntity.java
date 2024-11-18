package de.ct.manager.schedule.util;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@MappedSuperclass
@Data
public abstract class AbstractPersistentEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @NotBlank
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Version
    private Integer version;
}
