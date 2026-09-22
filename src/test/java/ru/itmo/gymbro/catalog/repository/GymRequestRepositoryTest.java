package ru.itmo.gymbro.catalog.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.model.GymRequest;
import ru.itmo.gymbro.catalog.model.RequestStatus;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

class GymRequestRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private GymRequestRepository requests;

    @Autowired
    private GymRepository gyms;

    @Autowired
    private UserRepository users;

    @Test
    @DisplayName("Новая заявка сохраняется в статусе PENDING без зала и проверяющего")
    void savesPendingRequest() {
        long authorId = newUser("author@mail.ru");

        GymRequest saved = requests.save(GymRequest.submit(authorId, "Атлант", "Москва", "Ленина, 5"));

        GymRequest found = requests.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(found.getAuthorId()).isEqualTo(authorId);
        assertThat(found.getGymId()).isNull();
        assertThat(found.getReviewedBy()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Одобренная заявка помнит созданный зал и администратора")
    void storesApprovedRequestWithGym() {
        long authorId = newUser("author2@mail.ru");
        long adminId = newUser("admin2@mail.ru");
        GymRequest request = requests.save(GymRequest.submit(authorId, "Атлант", "Москва", "Ленина, 7"));
        Gym gym = gyms.save(request.toGym());

        request.approve(adminId, gym.getId());
        requests.save(request);

        GymRequest reloaded = requests.findById(request.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(reloaded.getGymId()).isEqualTo(gym.getId());
        assertThat(reloaded.getReviewedBy()).isEqualTo(adminId);
        assertThat(reloaded.isPending()).isFalse();
    }

    private long newUser(String email) {
        return users.save(User.register(email, "hash")).getId();
    }
}
