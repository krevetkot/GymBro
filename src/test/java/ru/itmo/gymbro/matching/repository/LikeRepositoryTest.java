package ru.itmo.gymbro.matching.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.matching.model.Like;

import static org.assertj.core.api.Assertions.assertThat;

class LikeRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private LikeRepository likes;

    @Autowired
    private UserRepository users;

    @Test
    @DisplayName("Лайк направленный: обратный не появляется сам собой")
    void savesDirectedLike() {
        long anna = newUser("anna-like@mail.ru");
        long boris = newUser("boris-like@mail.ru");

        Like saved = likes.save(Like.from(anna, boris));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(likes.existsBetween(anna, boris)).isTrue();
        assertThat(likes.existsBetween(boris, anna)).isFalse();
        assertThat(likes.findBetween(anna, boris)).isPresent();
    }

    @Test
    @DisplayName("Встречный лайк находится по обратной паре")
    void findsReciprocalLike() {
        long anna = newUser("anna-pair@mail.ru");
        long boris = newUser("boris-pair@mail.ru");
        likes.save(Like.from(anna, boris));

        likes.save(Like.from(boris, anna));

        assertThat(likes.existsBetween(anna, boris)).isTrue();
        assertThat(likes.existsBetween(boris, anna)).isTrue();
    }

    private long newUser(String email) {
        return users.save(User.register(email, "hash")).getId();
    }
}
