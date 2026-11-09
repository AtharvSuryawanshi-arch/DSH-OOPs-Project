package com.example.itsm.settings;

import com.example.itsm.incident.assignment.AssignmentStrategyType;

public record SettingsRequest(
        AssignmentStrategyType assignmentStrategy,
        Boolean autoAssign
) {
}
