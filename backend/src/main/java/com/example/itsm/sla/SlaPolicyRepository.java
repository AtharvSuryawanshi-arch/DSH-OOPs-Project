package com.example.itsm.sla;

import com.example.itsm.catalog.ItService;
import com.example.itsm.incident.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

    Optional<SlaPolicy> findByPriorityAndServiceIdAndActiveTrue(Priority priority, Long serviceId);

    Optional<SlaPolicy> findByPriorityAndServiceIsNullAndActiveTrue(Priority priority);

    List<SlaPolicy> findAllByOrderByPriorityAsc();

    boolean existsByPriorityAndServiceIsNull(Priority priority);

    boolean existsByPriorityAndServiceIsNullAndIdNot(Priority priority, Long id);

    boolean existsByPriorityAndService(Priority priority, ItService service);

    boolean existsByPriorityAndServiceAndIdNot(Priority priority, ItService service, Long id);
}
