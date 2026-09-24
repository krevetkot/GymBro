package ru.itmo.gymbro.catalog.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Sport;

import static org.assertj.core.api.Assertions.assertThat;

class SportRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private SportRepository sports;

    @Test
    @DisplayName("Миграция заполнила справочник видов спорта")
    void seedsSportsFromMigration() {
        Page<Sport> page = sports.findAll(PageRequest.of(0, 50));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(10);
        assertThat(page.getContent()).extracting(Sport::getName).contains("Бег", "Плавание");
    }

    @Test
    @DisplayName("Новый вид спорта сохраняется с обрезанными пробелами")
    void savesNewSport() {
        Sport saved = sports.save(Sport.of("  Сквош  "));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Сквош");
        assertThat(sports.existsByName("Сквош")).isTrue();
        assertThat(sports.existsByName("Кёрлинг")).isFalse();
    }
}
