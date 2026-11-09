package com.example.itsm.sla;

import com.example.itsm.catalog.CatalogService;
import com.example.itsm.catalog.ItService;
import com.example.itsm.common.BusinessRuleException;
import com.example.itsm.common.ResourceNotFoundException;
import com.example.itsm.incident.Priority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SlaPolicyService {

    private final SlaPolicyRepository slaPolicyRepository;
    private final CatalogService catalogService;

    public SlaPolicyService(SlaPolicyRepository slaPolicyRepository, CatalogService catalogService) {
        this.slaPolicyRepository = slaPolicyRepository;
        this.catalogService = catalogService;
    }

    @Transactional(readOnly = true)
    public List<SlaPolicyResponse> listActive() {
        return slaPolicyRepository.findAllByOrderByPriorityAsc().stream()
                .filter(SlaPolicy::isActive)
                .map(SlaPolicyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SlaPolicyResponse> listAll() {
        return slaPolicyRepository.findAllByOrderByPriorityAsc().stream()
                .map(SlaPolicyResponse::from)
                .toList();
    }

    @Transactional
    public SlaPolicyResponse create(SlaPolicyRequest request) {
        validateTargets(request);
        ItService service = request.serviceId() == null ? null : catalogService.requireService(request.serviceId());
        ensureUnique(request.priority(), service, null);

        SlaPolicy policy = new SlaPolicy(
                request.name().trim(),
                request.priority(),
                service,
                request.responseMinutes(),
                request.resolutionMinutes(),
                request.warningPercent()
        );
        if (request.active() != null) {
            policy.setActive(request.active());
        }
        return SlaPolicyResponse.from(slaPolicyRepository.save(policy));
    }

    @Transactional
    public SlaPolicyResponse update(Long id, SlaPolicyRequest request) {
        validateTargets(request);
        SlaPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("SLA policy", id));
        ItService service = request.serviceId() == null ? null : catalogService.requireService(request.serviceId());
        ensureUnique(request.priority(), service, id);

        policy.update(
                request.name().trim(),
                request.priority(),
                service,
                request.responseMinutes(),
                request.resolutionMinutes(),
                request.warningPercent()
        );
        if (request.active() != null) {
            policy.setActive(request.active());
        }
        return SlaPolicyResponse.from(policy);
    }

    @Transactional(readOnly = true)
    public SlaDeadlines deadlinesFor(Priority priority, ItService service, Instant createdAt) {
        return computeDeadlines(resolve(priority, service), createdAt);
    }

    @Transactional(readOnly = true)
    public SlaPolicy resolve(Priority priority, ItService service) {
        if (service != null) {
            var servicePolicy = slaPolicyRepository
                    .findByPriorityAndServiceIdAndActiveTrue(priority, service.getId());
            if (servicePolicy.isPresent()) {
                return servicePolicy.get();
            }
        }
        return slaPolicyRepository.findByPriorityAndServiceIsNullAndActiveTrue(priority)
                .orElseThrow(() -> new BusinessRuleException(
                        "SLA_POLICY_MISSING",
                        "No active SLA policy is configured for " + priority + " incidents."
                ));
    }

    public SlaDeadlines computeDeadlines(SlaPolicy policy, Instant createdAt) {
        long responseSeconds = policy.getResponseMinutes() * 60L;
        long resolutionSeconds = policy.getResolutionMinutes() * 60L;
        long warningOffset = policy.getWarningPercent() == 0 ? 80 : policy.getWarningPercent();

        Instant responseDue = createdAt.plusSeconds(responseSeconds);
        Instant resolutionDue = createdAt.plusSeconds(resolutionSeconds);
        Instant responseWarning = createdAt.plusSeconds(responseSeconds * warningOffset / 100);
        Instant resolutionWarning = createdAt.plusSeconds(resolutionSeconds * warningOffset / 100);

        return new SlaDeadlines(responseDue, resolutionDue, responseWarning, resolutionWarning);
    }

    private void ensureUnique(Priority priority, ItService service, Long currentId) {
        boolean duplicate = currentId == null
                ? (service == null
                    ? slaPolicyRepository.existsByPriorityAndServiceIsNull(priority)
                    : slaPolicyRepository.existsByPriorityAndService(priority, service))
                : (service == null
                    ? slaPolicyRepository.existsByPriorityAndServiceIsNullAndIdNot(priority, currentId)
                    : slaPolicyRepository.existsByPriorityAndServiceAndIdNot(priority, service, currentId));

        if (duplicate) {
            throw new BusinessRuleException(
                    "SLA_POLICY_DUPLICATE",
                    "An SLA policy already exists for " + priority
                            + (service == null ? " across all services." : " on " + service.getName() + ".")
            );
        }
    }

    private void validateTargets(SlaPolicyRequest request) {
        if (request.resolutionMinutes() <= request.responseMinutes()) {
            throw new BusinessRuleException(
                    "INVALID_SLA_TARGETS",
                    "The resolution target must be longer than the response target."
            );
        }
    }
}
