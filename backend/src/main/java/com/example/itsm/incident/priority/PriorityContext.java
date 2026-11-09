package com.example.itsm.incident.priority;

import com.example.itsm.catalog.BusinessCriticality;
import com.example.itsm.incident.Impact;
import com.example.itsm.incident.Urgency;

public record PriorityContext(
        Impact impact,
        Urgency urgency,
        BusinessCriticality businessCriticality,
        int affectedUsers
) {
}
