package com.example.itsm.incident;

import com.example.itsm.incident.dto.TechnicianWorkloadResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/technicians/me")
public class TechnicianWorkloadController {

    private final IncidentService incidentService;

    public TechnicianWorkloadController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/workload")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public TechnicianWorkloadResponse workload() {
        return incidentService.myWorkload();
    }
}
