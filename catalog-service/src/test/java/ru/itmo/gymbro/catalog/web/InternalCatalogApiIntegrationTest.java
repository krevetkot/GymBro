package ru.itmo.gymbro.catalog.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.model.Sport;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.SportRepository;

class InternalCatalogApiIntegrationTest extends AbstractIntegrationTest {

    private static final String INTERNAL = "/internal/v1/catalog";

    @Autowired
    private SportRepository sports;

    @Autowired
    private GymRepository gyms;

    @Test
    @DisplayName("Снимок вида спорта для соседних сервисов: id и название")
    void returnsSportSnapshot() {
        Sport sport = sports.save(Sport.of("Сквош")).block();

        client.get().uri(INTERNAL + "/sports/" + sport.getId()).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(sport.getId())
                .jsonPath("$.name").isEqualTo("Сквош");
    }

    @Test
    @DisplayName("Снимок зала для соседних сервисов: id, название, город и адрес")
    void returnsGymSnapshot() {
        Gym gym = gyms.save(Gym.of("Атлант", "Москва", "Ленина, 1")).block();

        client.get().uri(INTERNAL + "/gyms/" + gym.getId()).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(gym.getId())
                .jsonPath("$.name").isEqualTo("Атлант")
                .jsonPath("$.city").isEqualTo("Москва")
                .jsonPath("$.address").isEqualTo("Ленина, 1");
    }

    @Test
    @DisplayName("Несуществующие вид спорта и зал — 404, по нему соседи понимают, что ссылка неверна")
    void missingEntriesAreNotFound() {
        client.get().uri(INTERNAL + "/sports/" + Long.MAX_VALUE).exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.detail").isEqualTo("Вид спорта " + Long.MAX_VALUE + " не найден");
        client.get().uri(INTERNAL + "/gyms/" + Long.MAX_VALUE).exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.detail").isEqualTo("Зал " + Long.MAX_VALUE + " не найден");
    }

    @Test
    @DisplayName("Неположительный идентификатор — 400")
    void rejectsInvalidIdentifier() {
        client.get().uri(INTERNAL + "/sports/0").exchange().expectStatus().isBadRequest();
        client.get().uri(INTERNAL + "/gyms/-1").exchange().expectStatus().isBadRequest();
    }
}
