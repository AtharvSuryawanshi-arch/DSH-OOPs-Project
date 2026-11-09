package com.example.itsm.sla;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sla-policies")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSlaPolicyController {

    private final SlaPolicyService slaPolicyService;

    public AdminSlaPolicyController(SlaPolicyService slaPolicyService) {
        this.slaPolicyService = slaPolicyService;
    }

    @GetMapping
    public List<SlaPolicyResponse> list() {
        return slaPolicyService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SlaPolicyResponse create(@Valid @RequestBody SlaPolicyRequest request) {
        return slaPolicyService.create(request);
    }

    @PatchMapping("/{id}")
    public SlaPolicyResponse update(@PathVariable Long id, @Valid @RequestBody SlaPolicyRequest request) {
        return slaPolicyService.update(id, request);
    }
}
