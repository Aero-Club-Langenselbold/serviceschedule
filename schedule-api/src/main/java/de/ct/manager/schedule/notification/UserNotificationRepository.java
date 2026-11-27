package de.ct.manager.schedule.notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import de.ct.manager.schedule.maintenance.MaintenanceDefect;
import de.ct.manager.schedule.operations.OperationsChecklist;
import de.ct.manager.schedule.user.User;

public interface UserNotificationRepository extends JpaRepository<UserNotification, String> {

    @EntityGraph(attributePaths = { "actor", "defect", "defect.aircraft", "checklist", "checklist.aircraft" })
    List<UserNotification> findAllByUserOrderByCreatedAtDescIdDesc(User user, Pageable pageable);

    long countByUserAndReadAtIsNull(User user);

    Optional<UserNotification> findByIdAndUser(String id, User user);

    List<UserNotification> findAllByUserAndReadAtIsNull(User user);

    @Modifying
    @Query("DELETE FROM UserNotification n WHERE n.defect = :defect AND n.user = :user AND n.readAt IS NULL")
    void deleteUnreadByDefectAndUser(MaintenanceDefect defect, User user);

    @Modifying
    @Query("UPDATE UserNotification n SET n.readAt = :readAt WHERE n.checklist = :checklist AND n.readAt IS NULL")
    void markReadByChecklist(OperationsChecklist checklist, Instant readAt);

    @Modifying
    @Query("DELETE FROM UserNotification n WHERE n.checklist = :checklist AND n.readAt IS NULL")
    void deleteUnreadByChecklist(OperationsChecklist checklist);
}
