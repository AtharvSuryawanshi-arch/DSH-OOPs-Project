package com.example.itsm.incident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "duplicateOf"})
    @Query("select i from Incident i where i.id = :id")
    Optional<Incident> findDetailedById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "duplicateOf"})
    Page<Incident> findAll(Specification<Incident> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "duplicateOf"})
    @Query("select i from Incident i")
    List<Incident> findAllDetailed();

    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "duplicateOf"})
    List<Incident> findByStatusIn(Collection<IncidentStatus> statuses);

    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "duplicateOf"})
    List<Incident> findByAssignedTechnicianIdAndStatusInOrderByResolutionDueAtAsc(
            Long technicianId,
            Collection<IncidentStatus> statuses
    );

    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "duplicateOf"})
    List<Incident> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "duplicateOf"})
    List<Incident> findByAssignedTechnicianIdOrderByCreatedAtDesc(Long technicianId);

    @EntityGraph(attributePaths = {"reporter", "category", "service", "assignedTechnician", "duplicateOf"})
    @Query("""
            select i from Incident i
            where i.status in :resolvedStatuses
              and i.resolutionNotes is not null
              and i.createdAt >= :since
              and i.id <> :excludeId
            """)
    List<Incident> findResolvedForSuggestions(
            @Param("resolvedStatuses") Collection<IncidentStatus> resolvedStatuses,
            @Param("since") Instant since,
            @Param("excludeId") Long excludeId
    );

    /**
     * Candidates for duplicate detection, filtered in the database (status, age, not the incident
     * itself) and capped by {@code pageable}; only the text similarity is computed in Java.
     */
    @EntityGraph(attributePaths = {"reporter", "service", "assignedTechnician"})
    @Query("""
            select i from Incident i
            where i.status in :statuses
              and i.createdAt > :since
              and i.id <> :excludeId
            order by i.createdAt desc
            """)
    List<Incident> findDuplicateCandidates(
            @Param("statuses") Collection<IncidentStatus> statuses,
            @Param("since") Instant since,
            @Param("excludeId") Long excludeId,
            Pageable pageable
    );

    @Query(value = "select nextval('incident_reference_seq')", nativeQuery = true)
    long nextReferenceSequence();

    long countByStatusIn(Collection<IncidentStatus> statuses);

    long countByPriority(Priority priority);

    long countByReporterId(Long reporterId);

    long countByReporterIdAndStatusIn(Long reporterId, Collection<IncidentStatus> statuses);

    long countByAssignedTechnicianIdAndStatusIn(Long technicianId, Collection<IncidentStatus> statuses);

    long countByAssignedTechnicianIdAndServiceIdAndStatusIn(
            Long technicianId,
            Long serviceId,
            Collection<IncidentStatus> statuses
    );

    long countByCreatedAtAfter(Instant since);
}
