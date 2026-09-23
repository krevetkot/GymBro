package ru.itmo.gymbro.shared.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.data.relational.core.conversion.MutableAggregateChange;
import org.springframework.data.relational.core.mapping.event.BeforeSaveCallback;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
class ValidateBeforeSaveCallback implements BeforeSaveCallback<Object> {

    private final Validator validator;

    ValidateBeforeSaveCallback(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Object onBeforeSave(Object aggregate, MutableAggregateChange<Object> aggregateChange) {
        Set<ConstraintViolation<Object>> violations = validator.validate(aggregate);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return aggregate;
    }
}
