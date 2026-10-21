package ru.itmo.gymbro;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Запросы через WebTestClient выполняются на потоках Reactor и коммитятся сами, поэтому
 * откат тестовой транзакции их не покрывает: данные тестов удаляются явно, остаётся
 * только администратор из миграции.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private ApplicationContext context;

    protected WebTestClient client;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void setUpClient() {
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @AfterEach
    void deleteTestUsers() {
        jdbc.update("DELETE FROM users WHERE email <> 'admin@gymbro.local'");
    }
}
