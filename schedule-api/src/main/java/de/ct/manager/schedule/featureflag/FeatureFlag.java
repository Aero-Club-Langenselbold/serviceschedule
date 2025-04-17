package de.ct.manager.schedule.featureflag;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Table(name = "feature_flags_features")
@Entity
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class FeatureFlag implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "feat_uid")
    private String id;

    @Column(name = "enable", columnDefinition = "INTEGER", nullable = false)
    private int active;

    @Column
    private String description;

    @Column
    private String strategy;

    @Column
    private String expression;

    @Column
    private String groupname;

    public boolean isActive() {
        return active == 1;
    }
}

