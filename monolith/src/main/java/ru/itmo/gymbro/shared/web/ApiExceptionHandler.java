package ru.itmo.gymbro.shared.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;
import ru.itmo.gymbro.shared.dto.FieldViolation;

import java.util.List;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String INVALID_REQUEST = "Некорректный запрос";

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Не найдено", exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Конфликт", exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail handleForbidden(ForbiddenException exception) {
        return problem(HttpStatus.FORBIDDEN, "Доступ запрещён", exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    ProblemDetail handleUnauthorized(UnauthorizedException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Требуется аутентификация", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, INVALID_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException exception) {
        return problem(HttpStatus.CONFLICT, "Операция недопустима", exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return invalidFields(HttpStatus.BAD_REQUEST, violations);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        return problem(HttpStatus.CONFLICT, "Конфликт",
                "Данные противоречат уже сохранённым: запись с такими значениями существует или на неё есть ссылки");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        logger.error("Необработанная ошибка", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера",
                "Что-то пошло не так. Попробуйте повторить запрос позже");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return handleExceptionInternal(exception, invalidFields(status, violations), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldViolation(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return handleExceptionInternal(exception, invalidFields(status, violations), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(status, INVALID_REQUEST,
                "Тело запроса не удалось прочитать: проверьте формат JSON и типы полей");
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(status, INVALID_REQUEST,
                "Параметр '%s' имеет неверный формат".formatted(exception.getPropertyName()));
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    private static ProblemDetail invalidFields(HttpStatusCode status, List<FieldViolation> violations) {
        ProblemDetail problem = problem(status, INVALID_REQUEST, "Запрос содержит некорректные поля");
        problem.setProperty("errors", violations);
        return problem;
    }

    private static ProblemDetail problem(HttpStatusCode status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
