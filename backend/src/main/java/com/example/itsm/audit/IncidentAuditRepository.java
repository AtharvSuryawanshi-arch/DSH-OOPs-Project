package com.example.itsm.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentAuditRepository extends JpaRepository<IncidentAudit, Long> {

    @EntityGraph(attributePaths = {"actor"})
    List<IncidentAudit> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);

    @EntityGraph(attributePaths = {"actor", "incident"})
    Page<IncidentAudit> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
