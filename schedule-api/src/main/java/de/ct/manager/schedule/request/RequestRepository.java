package de.ct.manager.schedule.request;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import de.ct.manager.schedule.participant.Participant;


public interface RequestRepository extends JpaRepository<Request, String> {

    List<Request> findAllByAuthor(Sort sort, Participant author);

    Page<Request> findAllByAuthor(Pageable pageable, Participant author);
}
