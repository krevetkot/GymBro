package ru.itmo.gymbro.shared.dto;

import lombok.Getter;

@Getter
public class FieldViolation {

    private final String field;
    private final String message;

    public FieldViolation(String field, String message) {
        this.field = field;
        this.message = message;
    }
}
