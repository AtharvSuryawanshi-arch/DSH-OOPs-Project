package com.example.itsm.sla;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/sla")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSlaMonitorController {

    private final SlaMonitorService monitorService;

    public AdminSlaMonitorController(SlaMonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @PostMapping("/scan")
    public Map<String, String> scan() {
        monitorService.scan();
        return Map.of("status", "completed");
    }
}
