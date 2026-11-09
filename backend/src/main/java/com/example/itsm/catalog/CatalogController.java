package com.example.itsm.catalog;

import com.example.itsm.sla.SlaPolicyResponse;
import com.example.itsm.sla.SlaPolicyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;
    private final SlaPolicyService slaPolicyService;

    public CatalogController(CatalogService catalogService, SlaPolicyService slaPolicyService) {
        this.catalogService = catalogService;
        this.slaPolicyService = slaPolicyService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return catalogService.listActiveCategories();
    }

    @GetMapping("/services")
    public List<ServiceResponse> services() {
        return catalogService.listActiveServices();
    }

    @GetMapping("/sla-policies")
    public List<SlaPolicyResponse> slaPolicies() {
        return slaPolicyService.listActive();
    }
}
