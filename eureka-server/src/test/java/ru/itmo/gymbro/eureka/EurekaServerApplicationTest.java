package ru.itmo.gymbro.eureka;

import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaServerApplicationTest {

    @Autowired
    private PeerAwareInstanceRegistry registry;

    @Test
    @DisplayName("Реестр поднимается пустым: сам Eureka-сервер в нём не регистрируется")
    void startsWithEmptyRegistry() {
        assertThat(registry.getSortedApplications()).isEmpty();
    }
}
