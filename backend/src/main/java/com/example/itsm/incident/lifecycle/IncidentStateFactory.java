package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.IncidentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class IncidentStateFactory {

    private final Map<IncidentStatus, IncidentState> states = new EnumMap<>(IncidentStatus.class);

    public IncidentStateFactory() {
        register(new NewState());
        register(new ClassifiedState());
        register(new AssignedState());
        register(new AcknowledgedState());
        register(new InProgressState());
        register(new ResolvedState());
        register(new VerifiedState());
        register(new ClosedState());
        register(new CancelledState());
    }

    public IncidentState stateFor(IncidentStatus status) {
        IncidentState state = states.get(status);
        if (state == null) {
            throw new IllegalArgumentException("No state handler registered for " + status);
        }
        return state;
    }

    private void register(IncidentState state) {
        states.put(state.status(), state);
    }
}
