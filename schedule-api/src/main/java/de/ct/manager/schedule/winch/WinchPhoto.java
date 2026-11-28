package de.ct.manager.schedule.winch;

import java.time.Instant;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.util.AbstractPersistentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** A photo of a rope on a day; the image is a {@link WinchPhotoContent}, so listing photos does not load it. */
@Entity
@Table(name = "winch_photo")
@Data
@EqualsAndHashCode(callSuper = true)
public class WinchPhoto extends AbstractPersistentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "day_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WinchDay day;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rope_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WinchRope rope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User uploadedBy;

    @Column(nullable = false)
    private Instant uploadedAt = Instant.now();

    @Column(length = 255)
    private String filename;

    @Column(length = 50, nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long size;
}
