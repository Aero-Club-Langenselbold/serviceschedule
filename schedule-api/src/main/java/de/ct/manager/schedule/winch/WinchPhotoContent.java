package de.ct.manager.schedule.winch;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** The image of a {@link WinchPhoto}. */
@Entity
@Table(name = "winch_photo_content")
@Data
@EqualsAndHashCode(callSuper = true)
public class WinchPhotoContent extends AbstractPersistentEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "photo_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WinchPhoto photo;

    /** bytea in PostgreSQL; the length only matters for the schema Hibernate creates in the tests (H2). */
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, length = WinchBookService.MAX_PHOTO_BYTES)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private byte[] data;
}
