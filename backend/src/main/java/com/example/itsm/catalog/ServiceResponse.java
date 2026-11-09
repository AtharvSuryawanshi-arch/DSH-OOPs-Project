package com.example.itsm.catalog;

public record ServiceResponse(
        Long id,
        String code,
        String name,
        String description,
        BusinessCriticality businessCriticality,
        boolean active
) {

    public static ServiceResponse from(ItService service) {
        return new ServiceResponse(
                service.getId(),
                service.getCode(),
                service.getName(),
                service.getDescription(),
                service.getBusinessCriticality(),
                service.isActive()
        );
    }
}
