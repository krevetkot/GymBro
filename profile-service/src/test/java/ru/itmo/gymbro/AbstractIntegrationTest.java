package ru.itmo.gymbro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import jakarta.servlet.Filter;

@SpringBootTest
@Transactional
@Import(AbstractIntegrationTest.NeighbourReplacements.class)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected TestUsers testUsers;

    @Autowired
    protected TestCatalog testCatalog;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("gymbro.jwt.secret", () -> TestTokens.SECRET);
    }

    /** MockMvc с цепочкой фильтров Spring Security: запросы проходят проверку JWT, как в работающем сервисе. */
    protected static MockMvc securedMvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class NeighbourReplacements {

        @Bean
        @Primary
        TestUsers testUsers() {
            return new TestUsers();
        }

        @Bean
        @Primary
        TestCatalog testCatalog() {
            return new TestCatalog();
        }
    }
}
