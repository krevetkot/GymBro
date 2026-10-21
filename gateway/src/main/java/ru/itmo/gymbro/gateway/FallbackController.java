package ru.itmo.gymbro.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Ответ gateway, когда Circuit Breaker маршрута не пропустил запрос к сервису:
 * сервис не отвечает, не зарегистрирован в Eureka или после серии сбоев запросы к нему приостановлены.
 */
@RestController
class FallbackController {

    private static final Map<String, String> SERVICES = Map.of(
            "identity", "пользователей",
            "catalog", "каталога",
            "profile", "анкет",
            "matching", "ленты и мэтчей");

    @RequestMapping("/fallback/{service}")
    ResponseEntity<ProblemDetail> fallback(@PathVariable String service) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Сервис " + SERVICES.getOrDefault(service, service) + " временно недоступен, повторите запрос позже");
        problem.setTitle("Сервис недоступен");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
