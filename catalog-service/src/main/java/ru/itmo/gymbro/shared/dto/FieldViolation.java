package ru.itmo.gymbro.shared.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FieldViolation {

    private final String field;
    private final String message;
}
