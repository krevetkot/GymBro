package ru.itmo.gymbro.catalog.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;

import static org.assertj.core.api.Assertions.assertThat;

class GymRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private GymRepository gyms;

    @Test
    @DisplayName("Зал сохраняется и читается обратно")
    void savesAndReadsGym() {
        Gym saved = gyms.save(Gym.of("Спортлайф", "Санкт-Петербург", "Невский проспект, 1")).block();

        assertThat(saved.getId()).isNotNull();
        StepVerifier.create(gyms.findById(saved.getId()))
                .assertNext(found -> {
                    assertThat(found.getName()).isEqualTo("Спортлайф");
                    assertThat(found.getCity()).isEqualTo("Санкт-Петербург");
                    assertThat(found.getAddress()).isEqualTo("Невский проспект, 1");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Зал узнаётся по городу и адресу")
    void checksGymByCityAndAddress() {
        gyms.save(Gym.of("Атлант", "Москва", "Тверская, 10")).block();

        StepVerifier.create(gyms.existsByCityAndAddress("Москва", "Тверская, 10")).expectNext(true).verifyComplete();
        StepVerifier.create(gyms.existsByCityAndAddress("Москва", "Тверская, 11")).expectNext(false).verifyComplete();
    }
}
