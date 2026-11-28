package de.ct.manager.schedule.winch;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WinchRopeRepository extends JpaRepository<WinchRope, String> {

    List<WinchRope> findAllByOrderByPositionAscNameAsc();
}
