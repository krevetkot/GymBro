package ru.itmo.gymbro.shared.client;

import feign.FeignException;
import feign.Request;
import feign.Response;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.shared.api.DependencyUnavailableException;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteCallsTest {

    private static final String SERVICE = "соседа";

    @Test
    @DisplayName("Успешный ответ соседа возвращается как есть")
    void returnsResult() {
        assertThat(RemoteCalls.call(SERVICE, "не найдено", Mono.just("ответ")).block()).isEqualTo("ответ");
    }

    @Test
    @DisplayName("Пустой ответ соседа — сервис недоступен")
    void emptyResultMeansUnavailable() {
        assertThatThrownBy(() -> RemoteCalls.call(SERVICE, "не найдено", Mono.<String>empty()).block())
                .isInstanceOf(DependencyUnavailableException.class)
                .hasMessage("Сервис соседа вернул пустой ответ");
    }

    @ParameterizedTest
    @CsvSource({
            "400, java.lang.IllegalArgumentException",
            "401, ru.itmo.gymbro.shared.api.UnauthorizedException",
            "403, ru.itmo.gymbro.shared.api.ForbiddenException",
            "404, ru.itmo.gymbro.shared.api.NotFoundException",
            "500, ru.itmo.gymbro.shared.api.DependencyUnavailableException",
            "503, ru.itmo.gymbro.shared.api.DependencyUnavailableException"})
    @DisplayName("Ответ соседа с ошибкой переводится в доменное исключение по статусу")
    void translatesStatus(int status, Class<? extends RuntimeException> expected) {
        assertThatThrownBy(() -> RemoteCalls.call(SERVICE, "Запись не найдена", Mono.error(feignError(status))).block()).isInstanceOf(expected);
    }

    @Test
    @DisplayName("Для 404 используется текст ошибки вызывающего")
    void notFoundUsesCallerMessage() {
        assertThatThrownBy(() -> RemoteCalls.call(SERVICE, "Вид спорта 5 не найден", Mono.error(feignError(404))).block()).isInstanceOf(NotFoundException.class).hasMessage("Вид спорта 5 не найден");
    }

    @Test
    @DisplayName("Ошибка, завёрнутая Circuit Breaker, разворачивается и переводится так же")
    void unwrapsCircuitBreakerFailure() {
        assertThatThrownBy(() -> RemoteCalls.call(SERVICE, "Пользователь не найден", Mono.error(new NoFallbackAvailableException("no fallback", feignError(404)))).block()).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> RemoteCalls.call(SERVICE, "Пользователь не найден", Mono.error(new NoFallbackAvailableException("no fallback", new TimeoutException("too slow")))).block()).isInstanceOf(DependencyUnavailableException.class).hasMessage("Сервис соседа временно недоступен");
    }

    @Test
    @DisplayName("Открытый автомат — 503 с пояснением, что запросы приостановлены")
    void openBreakerMeansSuspended() {
        CallNotPermittedException open =
                CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("neighbour"));

        assertThatThrownBy(() -> RemoteCalls.call(SERVICE, "не найдено", Mono.error(new NoFallbackAvailableException("no fallback", open))).block()).isInstanceOf(DependencyUnavailableException.class)
                .hasMessageContaining("запросы к нему приостановлены")
                .hasCause(open);
    }

    @Test
    @DisplayName("Права и аутентификацию сосед проверяет сам: 401 и 403 не превращаются в 503")
    void keepsAuthErrors() {
        assertThatThrownBy(() -> RemoteCalls.call(SERVICE, "не найдено", Mono.error(feignError(401))).block()).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> RemoteCalls.call(SERVICE, "не найдено", Mono.error(feignError(403))).block()).isInstanceOf(ForbiddenException.class);
    }

    private static FeignException feignError(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://neighbour/internal/v1/test",
                Map.of(), new byte[0], StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(status)
                .reason("test")
                .request(request)
                .headers(Map.of())
                .build();
        return FeignException.errorStatus("NeighbourClient#call()", response);
    }
}
