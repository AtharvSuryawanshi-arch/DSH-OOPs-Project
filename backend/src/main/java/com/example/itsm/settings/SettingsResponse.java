package com.example.itsm.settings;

import com.example.itsm.incident.assignment.AssignmentStrategyType;

public record SettingsResponse(
        AssignmentStrategyType assignmentStrategy,
        boolean autoAssign
) {
}
