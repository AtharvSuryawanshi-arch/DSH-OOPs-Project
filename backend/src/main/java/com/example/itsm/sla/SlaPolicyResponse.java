package com.example.itsm.sla;

public record SlaPolicyResponse(
        Long id,
        String name,
        String priority,
        Long serviceId,
        String serviceName,
        int responseMinutes,
        int resolutionMinutes,
        int warningPercent,
        boolean active
) {

    public static SlaPolicyResponse from(SlaPolicy policy) {
        return new SlaPolicyResponse(
                policy.getId(),
                policy.getName(),
                policy.getPriority().name(),
                policy.getService() == null ? null : policy.getService().getId(),
                policy.getService() == null ? null : policy.getService().getName(),
                policy.getResponseMinutes(),
                policy.getResolutionMinutes(),
                policy.getWarningPercent(),
                policy.isActive()
        );
    }
}
