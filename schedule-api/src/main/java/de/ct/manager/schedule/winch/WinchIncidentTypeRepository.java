package de.ct.manager.schedule.winch;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WinchIncidentTypeRepository extends JpaRepository<WinchIncidentType, String> {

    List<WinchIncidentType> findAllByOrderByPositionAscNameAsc();
}
