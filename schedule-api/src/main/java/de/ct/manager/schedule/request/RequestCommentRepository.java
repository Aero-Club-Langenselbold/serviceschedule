package de.ct.manager.schedule.request;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestCommentRepository extends JpaRepository<RequestComment, String> {

    @EntityGraph(attributePaths = "author")
    List<RequestComment> findAllByRequestOrderByCreatedAtAsc(Request request);
}
