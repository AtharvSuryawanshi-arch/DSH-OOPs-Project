package com.example.itsm.user;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/technicians")
public class TechnicianDirectoryController {

    private final AdminUserService adminUserService;

    public TechnicianDirectoryController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    public List<UserResponse> listTechnicians() {
        return adminUserService.listTechnicians();
    }
}
