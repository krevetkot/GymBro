package ru.itmo.gymbro.catalog.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.model.GymRequest;
import ru.itmo.gymbro.catalog.model.RequestStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GymRequestRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private GymRequestRepository requests;

    @Autowired
    private GymRepository gyms;

    @Test
    @DisplayName("Новая заявка сохраняется в статусе PENDING без зала и проверяющего")
    void savesPendingRequest() {
        long authorId = testUsers.active();

        GymRequest saved = requests.save(GymRequest.submit(authorId, "Атлант", "Москва", "Ленина, 5")).block();

        StepVerifier.create(requests.findById(saved.getId()))
                .assertNext(found -> {
                    assertThat(found.getStatus()).isEqualTo(RequestStatus.PENDING);
                    assertThat(found.getAuthorId()).isEqualTo(authorId);
                    assertThat(found.getGymId()).isNull();
                    assertThat(found.getReviewedBy()).isNull();
                    assertThat(found.getCreatedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Одобренная заявка помнит созданный зал и администратора")
    void storesApprovedRequestWithGym() {
        long authorId = testUsers.active();
        long adminId = testUsers.admin();
        GymRequest request = requests.save(GymRequest.submit(authorId, "Атлант", "Москва", "Ленина, 7")).block();
        Gym gym = gyms.save(request.toGym()).block();

        request.approve(adminId, gym.getId());
        requests.save(request).block();

        StepVerifier.create(requests.findById(request.getId()))
                .assertNext(reloaded -> {
                    assertThat(reloaded.getStatus()).isEqualTo(RequestStatus.APPROVED);
                    assertThat(reloaded.getGymId()).isEqualTo(gym.getId());
                    assertThat(reloaded.getReviewedBy()).isEqualTo(adminId);
                    assertThat(reloaded.isPending()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Выборка под блокировку находит заявку и пуста для несуществующей")
    void findsRequestForUpdate() {
        GymRequest saved = requests.save(GymRequest.submit(testUsers.active(), "Атлант", "Москва", "Ленина, 9")).block();

        StepVerifier.create(requests.findByIdForUpdate(saved.getId()))
                .assertNext(found -> assertThat(found.getAddress()).isEqualTo("Ленина, 9"))
                .verifyComplete();
        StepVerifier.create(requests.findByIdForUpdate(Long.MAX_VALUE)).verifyComplete();
    }
}
