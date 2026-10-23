package ru.itmo.gymbro;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;

@SpringBootTest
@Import(AbstractIntegrationTest.IdentityReplacement.class)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected TestUsers testUsers;

    @Autowired
    protected DatabaseClient database;

    @Autowired
    private ApplicationContext context;

    protected WebTestClient client;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://%s:%d/%s".formatted(
                POSTGRES.getHost(), POSTGRES.getFirstMappedPort(), POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
        registry.add("spring.liquibase.url", POSTGRES::getJdbcUrl);
        registry.add("spring.liquibase.user", POSTGRES::getUsername);
        registry.add("spring.liquibase.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void setUpClient() {
        client = WebTestClient.bindToApplicationContext(context).configureClient()
                .responseTimeout(Duration.ofSeconds(30))
                .build();
    }

    @AfterEach
    void deleteTestData() {
        database.sql("DELETE FROM gym_requests").then()
                .then(database.sql("DELETE FROM gyms").then())
                .then(database.sql("DELETE FROM sports WHERE id > (SELECT MIN(id) + 9 FROM sports)").then())
                .block();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class IdentityReplacement {

        @Bean
        @Primary
        TestUsers testUsers() {
            return new TestUsers();
        }
    }
}
