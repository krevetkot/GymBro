package ru.itmo.gymbro.shared.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
class EntityValidationCallback implements BeforeConvertCallback<Object> {

    private final Validator validator;

    EntityValidationCallback(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Publisher<Object> onBeforeConvert(Object entity, SqlIdentifier table) {
        Set<ConstraintViolation<Object>> violations = validator.validate(entity);
        return violations.isEmpty() ? Mono.just(entity) : Mono.error(new ConstraintViolationException(violations));
    }
}
