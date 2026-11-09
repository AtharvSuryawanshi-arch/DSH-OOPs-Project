package com.example.itsm.incident.priority;

import com.example.itsm.incident.Priority;

public record PriorityResult(
        Priority priority,
        int score,
        String rationale
) {
}
