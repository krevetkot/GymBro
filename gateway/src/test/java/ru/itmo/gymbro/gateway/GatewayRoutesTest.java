package ru.itmo.gymbro.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTest {

    @Autowired
    private RouteLocator routes;

    @Test
    @DisplayName("API и Swagger маршрутизируются на монолит через Eureka")
    void routesApiAndDocsToMonolith() {
        List<Route> all = routes.getRoutes().collectList().block();

        assertThat(all).extracting(Route::getId).containsExactlyInAnyOrder("monolith-api", "monolith-docs");
        assertThat(all).extracting(Route::getUri).containsOnly(URI.create("lb://monolith"));
    }
}
