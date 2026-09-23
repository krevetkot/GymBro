package ru.itmo.gymbro.shared.dto;

public class FieldViolation {

    private final String field;
    private final String message;

    public FieldViolation(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }
}
