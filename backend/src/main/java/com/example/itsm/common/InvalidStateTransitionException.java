package com.example.itsm.common;

import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends BusinessRuleException {

    public InvalidStateTransitionException(String from, String to) {
        super(
                "INVALID_STATE_TRANSITION",
                "Incident cannot move from " + from + " to " + to + ".",
                HttpStatus.CONFLICT
        );
    }

    private InvalidStateTransitionException(String message) {
        super("INVALID_STATE_TRANSITION", message, HttpStatus.CONFLICT);
    }

    /** For an action the current state does not support, e.g. "You cannot verify an incident that is Assigned." */
    public static InvalidStateTransitionException forAction(String action, String currentStatusLabel) {
        return new InvalidStateTransitionException(
                "You cannot " + action + " an incident that is " + currentStatusLabel + "."
        );
    }
}
