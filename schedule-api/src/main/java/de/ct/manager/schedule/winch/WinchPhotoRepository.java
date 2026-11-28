package de.ct.manager.schedule.winch;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WinchPhotoRepository extends JpaRepository<WinchPhoto, String> {

    @EntityGraph(attributePaths = { "rope", "uploadedBy" })
    List<WinchPhoto> findAllByDayOrderByUploadedAt(WinchDay day);

    boolean existsByDay(WinchDay day);

    boolean existsByRope(WinchRope rope);

    @Query("SELECT DISTINCT p.rope.id FROM WinchPhoto p")
    List<String> findUsedRopeIds();

    @Query("SELECT p.day.id AS dayId, COUNT(p) AS total FROM WinchPhoto p WHERE p.day IN :days GROUP BY p.day.id")
    List<DayCount> countByDay(Collection<WinchDay> days);

    interface DayCount {
        String getDayId();
        long getTotal();
    }
}
