package ru.itmo.gymbro.catalog.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;

import static org.assertj.core.api.Assertions.assertThat;

class GymRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private GymRepository gyms;

    @Test
    @DisplayName("Зал сохраняется и читается обратно")
    void savesAndReadsGym() {
        Gym saved = gyms.save(Gym.of("Спортлайф", "Санкт-Петербург", "Невский проспект, 1"));

        assertThat(saved.getId()).isNotNull();

        Gym found = gyms.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Спортлайф");
        assertThat(found.getCity()).isEqualTo("Санкт-Петербург");
        assertThat(found.getAddress()).isEqualTo("Невский проспект, 1");
    }

    @Test
    @DisplayName("Зал узнаётся по городу и адресу")
    void checksGymByCityAndAddress() {
        gyms.save(Gym.of("Атлант", "Москва", "Тверская, 10"));

        assertThat(gyms.existsByCityAndAddress("Москва", "Тверская, 10")).isTrue();
        assertThat(gyms.existsByCityAndAddress("Москва", "Тверская, 11")).isFalse();
    }
}
