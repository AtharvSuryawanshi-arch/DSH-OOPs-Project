package com.example.itsm.incident.assignment;

import com.example.itsm.incident.Incident;
import com.example.itsm.user.Technician;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssignmentStrategyTest {

    private final Incident incident = mock(Incident.class);

    private Technician technician(long id, boolean available, int capacity, int experience) {
        Technician technician = mock(Technician.class);
        when(technician.getId()).thenReturn(id);
        when(technician.getName()).thenReturn("Technician " + id);
        when(technician.isAvailable()).thenReturn(available);
        when(technician.getMaxCapacity()).thenReturn(capacity);
        when(technician.getExperienceYears()).thenReturn(experience);
        return technician;
    }

    @Test
    void skillBasedStrategyPrefersACategoryMatch() {
        AssignmentCandidate skilled = new AssignmentCandidate(
                technician(1, true, 5, 3), 4, 1, true
        );
        AssignmentCandidate lessBusy = new AssignmentCandidate(
                technician(2, true, 5, 8), 0, 0, false
        );

        var selected = new SkillBasedAssignmentStrategy().select(incident, List.of(lessBusy, skilled));

        assertThat(selected).isPresent();
        assertThat(selected.orElseThrow().technician().getId()).isEqualTo(1L);
    }

    @Test
    void loadBasedStrategyPrefersTheLeastBusyTechnician() {
        AssignmentCandidate busy = new AssignmentCandidate(
                technician(1, true, 5, 3), 4, 2, true
        );
        AssignmentCandidate free = new AssignmentCandidate(
                technician(2, true, 5, 1), 1, 0, false
        );

        var selected = new LoadBasedAssignmentStrategy().select(incident, List.of(busy, free));

        assertThat(selected).isPresent();
        assertThat(selected.orElseThrow().technician().getId()).isEqualTo(2L);
    }

    @Test
    void roundRobinRotatesAcrossAvailableTechnicians() {
        RoundRobinAssignmentStrategy strategy = new RoundRobinAssignmentStrategy();
        List<AssignmentCandidate> candidates = List.of(
                new AssignmentCandidate(technician(1, true, 5, 1), 0, 0, false),
                new AssignmentCandidate(technician(2, true, 5, 1), 0, 0, false)
        );

        Long first = strategy.select(incident, candidates).orElseThrow().technician().getId();
        Long second = strategy.select(incident, candidates).orElseThrow().technician().getId();
        Long third = strategy.select(incident, candidates).orElseThrow().technician().getId();

        assertThat(first).isNotEqualTo(second);
        assertThat(third).isEqualTo(first);
    }

    @Test
    void strategiesSkipTechniciansAtCapacity() {
        AssignmentCandidate full = new AssignmentCandidate(
                technician(1, true, 2, 8), 2, 0, true
        );
        AssignmentCandidate unavailable = new AssignmentCandidate(
                technician(2, false, 5, 8), 0, 0, true
        );

        assertThat(new SkillBasedAssignmentStrategy().select(incident, List.of(full, unavailable)))
                .isEmpty();
        assertThat(new LoadBasedAssignmentStrategy().select(incident, List.of(full, unavailable)))
                .isEmpty();
    }
}
