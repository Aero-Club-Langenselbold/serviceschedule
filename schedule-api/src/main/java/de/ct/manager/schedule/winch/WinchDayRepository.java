package de.ct.manager.schedule.winch;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WinchDayRepository extends JpaRepository<WinchDay, String> {

    @EntityGraph(attributePaths = { "remarkUpdatedBy", "drivers" })
    Optional<WinchDay> findByDate(LocalDate date);

    Page<WinchDay> findAllByDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    @EntityGraph(attributePaths = { "drivers" })
    List<WinchDay> findAllWithDriversByIdIn(List<String> ids);

    @Query("SELECT DISTINCT YEAR(d.date) FROM WinchDay d")
    List<Integer> findYears();
}
