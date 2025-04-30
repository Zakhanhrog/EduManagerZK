package com.eduzk.model.exceptions;

public class ScheduleConflictException extends Exception {

    public ScheduleConflictException(String message) {
        super(message);
    }

    public ScheduleConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}