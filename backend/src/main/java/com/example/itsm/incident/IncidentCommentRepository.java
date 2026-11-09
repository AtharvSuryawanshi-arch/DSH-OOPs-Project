package com.example.itsm.incident;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentCommentRepository extends JpaRepository<IncidentComment, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<IncidentComment> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);
}
