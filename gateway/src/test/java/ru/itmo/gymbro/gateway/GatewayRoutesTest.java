package ru.itmo.gymbro.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTest {

    @Autowired
    private RouteLocator routes;

    @Test
    @DisplayName("API и Swagger каждого сервиса маршрутизируются на него через Eureka")
    void routesEachServiceThroughEureka() {
        Map<String, URI> targets = allRoutes().stream().collect(Collectors.toMap(Route::getId, Route::getUri));

        assertThat(targets).containsExactlyInAnyOrderEntriesOf(Map.of(
                "identity-api", URI.create("lb://identity-service"),
                "identity-docs", URI.create("lb://identity-service"),
                "monolith-api", URI.create("lb://monolith"),
                "monolith-docs", URI.create("lb://monolith")));
    }

    @Test
    @DisplayName("Маршрут пользователей проверяется раньше общего /api/** монолита")
    void identityRouteWinsOverMonolithCatchAll() {
        List<String> order = allRoutes().stream().map(Route::getId).toList();

        assertThat(order.indexOf("identity-api")).isLessThan(order.indexOf("monolith-api"));
    }

    private List<Route> allRoutes() {
        return routes.getRoutes().collectList().block();
    }
}
