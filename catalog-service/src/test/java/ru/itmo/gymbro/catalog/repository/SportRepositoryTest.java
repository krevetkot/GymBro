package ru.itmo.gymbro.catalog.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import reactor.test.StepVerifier;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Sport;

import static org.assertj.core.api.Assertions.assertThat;

class SportRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private SportRepository sports;

    @Test
    @DisplayName("Миграция заполнила справочник видов спорта")
    void seedsSportsFromMigration() {
        StepVerifier.create(sports.count())
                .assertNext(total -> assertThat(total).isGreaterThanOrEqualTo(10))
                .verifyComplete();
        StepVerifier.create(sports.findAllBy(PageRequest.of(0, 50)).map(Sport::getName).collectList())
                .assertNext(names -> assertThat(names).contains("Бег", "Плавание"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Страница отдаёт не больше size записей в заданном порядке")
    void pagesWithSort() {
        StepVerifier.create(sports.findAllBy(PageRequest.of(1, 3, Sort.by("id"))).map(Sport::getId).collectList())
                .assertNext(ids -> assertThat(ids).hasSize(3).isSorted())
                .verifyComplete();
    }

    @Test
    @DisplayName("Новый вид спорта сохраняется с обрезанными пробелами")
    void savesNewSport() {
        StepVerifier.create(sports.save(Sport.of("  Сквош  ")))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getName()).isEqualTo("Сквош");
                })
                .verifyComplete();
        StepVerifier.create(sports.existsByName("Сквош")).expectNext(true).verifyComplete();
        StepVerifier.create(sports.existsByName("Кёрлинг")).expectNext(false).verifyComplete();
    }
}
