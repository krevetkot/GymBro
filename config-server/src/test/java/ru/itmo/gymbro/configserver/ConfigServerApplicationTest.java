package ru.itmo.gymbro.configserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ConfigServerApplicationTest {

    @Autowired
    private EnvironmentRepository repository;

    @Test
    @DisplayName("Сервису отдаются его файл и общий application.yml из config-repo, свой файл важнее")
    void servesServiceAndSharedConfig() {
        Environment environment = repository.findOne("matching-service", "default", null);

        List<String> sources = environment.getPropertySources().stream().map(PropertySource::getName).toList();
        assertThat(sources).hasSize(2);
        assertThat(sources.get(0)).endsWith("matching-service.yml");
        assertThat(sources.get(1)).endsWith("application.yml");
    }

    @Test
    @DisplayName("Плейсхолдеры переменных окружения отдаются как есть и раскрываются на стороне сервиса")
    void keepsEnvironmentPlaceholders() {
        Environment environment = repository.findOne("matching-service", "default", null);

        Object url = environment.getPropertySources().getFirst().getSource().get("spring.datasource.url");
        assertThat(url).asString().startsWith("${MATCHING_DB_URL:");
    }
}
