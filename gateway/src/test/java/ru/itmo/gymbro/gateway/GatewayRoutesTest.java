package ru.itmo.gymbro.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTest {

    @Autowired
    private RouteLocator routes;

    @Test
    @DisplayName("API и документация каждого сервиса маршрутизируются на него через Eureka")
    void routesEachServiceThroughEureka() {
        Map<String, URI> targets = allRoutes().stream().collect(Collectors.toMap(Route::getId, Route::getUri));

        Map<String, URI> expected = new HashMap<>();
        for (String service : List.of("identity", "catalog", "profile", "matching")) {
            expected.put(service + "-api", URI.create("lb://" + service + "-service"));
            expected.put(service + "-docs", URI.create("lb://" + service + "-service"));
        }
        assertThat(targets).containsExactlyInAnyOrderEntriesOf(expected);
    }

    private List<Route> allRoutes() {
        return routes.getRoutes().collectList().block();
    }
}
