package com.example.itsm.common;

public class InvalidStateTransitionException extends BusinessRuleException {

    public InvalidStateTransitionException(String from, String to) {
        super(
                "INVALID_STATE_TRANSITION",
                "Incident cannot move from " + from + " to " + to + ".",
                org.springframework.http.HttpStatus.CONFLICT
        );
    }
}
