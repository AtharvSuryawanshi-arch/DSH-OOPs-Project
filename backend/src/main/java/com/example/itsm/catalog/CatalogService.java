package com.example.itsm.catalog;

import com.example.itsm.common.BusinessRuleException;
import com.example.itsm.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ItServiceRepository serviceRepository;

    public CatalogService(CategoryRepository categoryRepository, ItServiceRepository serviceRepository) {
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAllCategories() {
        return categoryRepository.findAll().stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> listActiveServices() {
        return serviceRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(ServiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> listAllServices() {
        return serviceRepository.findAll().stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .map(ServiceResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String code = request.code().trim().toUpperCase();
        if (categoryRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new BusinessRuleException("CATEGORY_CODE_IN_USE", "A category with this code already exists.");
        }
        if (categoryRepository.findByNameIgnoreCase(request.name().trim()).isPresent()) {
            throw new BusinessRuleException("CATEGORY_NAME_IN_USE", "A category with this name already exists.");
        }
        Category category = new Category(code, request.name().trim(), request.description());
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        if (categoryRepository.findByNameIgnoreCase(request.name().trim())
                .filter(existing -> !existing.getId().equals(id))
                .isPresent()) {
            throw new BusinessRuleException("CATEGORY_NAME_IN_USE", "A category with this name already exists.");
        }
        category.update(request.name().trim(), request.description());
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return CategoryResponse.from(category);
    }

    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        String code = request.code().trim().toUpperCase();
        if (serviceRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new BusinessRuleException("SERVICE_CODE_IN_USE", "A service with this code already exists.");
        }
        if (serviceRepository.findByNameIgnoreCase(request.name().trim()).isPresent()) {
            throw new BusinessRuleException("SERVICE_NAME_IN_USE", "A service with this name already exists.");
        }
        ItService service = new ItService(
                code,
                request.name().trim(),
                request.description(),
                request.businessCriticality()
        );
        if (request.active() != null) {
            service.setActive(request.active());
        }
        return ServiceResponse.from(serviceRepository.save(service));
    }

    @Transactional
    public ServiceResponse updateService(Long id, ServiceRequest request) {
        ItService service = serviceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Service", id));
        if (serviceRepository.findByNameIgnoreCase(request.name().trim())
                .filter(existing -> !existing.getId().equals(id))
                .isPresent()) {
            throw new BusinessRuleException("SERVICE_NAME_IN_USE", "A service with this name already exists.");
        }
        service.update(request.name().trim(), request.description(), request.businessCriticality());
        if (request.active() != null) {
            service.setActive(request.active());
        }
        return ServiceResponse.from(service);
    }

    @Transactional(readOnly = true)
    public Category requireActiveCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        if (!category.isActive()) {
            throw new BusinessRuleException("CATEGORY_INACTIVE", "The selected category is inactive.");
        }
        return category;
    }

    @Transactional(readOnly = true)
    public ItService requireActiveService(Long id) {
        ItService service = serviceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Service", id));
        if (!service.isActive()) {
            throw new BusinessRuleException("SERVICE_INACTIVE", "The selected service is inactive.");
        }
        return service;
    }

    @Transactional(readOnly = true)
    public Category requireCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }

    @Transactional(readOnly = true)
    public ItService requireService(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Service", id));
    }
}
