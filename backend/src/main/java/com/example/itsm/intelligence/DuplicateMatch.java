package com.example.itsm.intelligence;

import com.example.itsm.incident.Incident;

public record DuplicateMatch(Incident incident, double score) {
}
