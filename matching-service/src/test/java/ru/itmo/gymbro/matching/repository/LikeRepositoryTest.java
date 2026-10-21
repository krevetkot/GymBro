package ru.itmo.gymbro.matching.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.matching.model.Like;

import static org.assertj.core.api.Assertions.assertThat;

class LikeRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private LikeRepository likes;

    @Test
    @DisplayName("Лайк направленный: обратный не появляется сам собой")
    void savesDirectedLike() {
        long anna = testUsers.active();
        long boris = testUsers.active();

        Like saved = likes.save(Like.from(anna, boris));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(likes.existsBetween(anna, boris)).isTrue();
        assertThat(likes.existsBetween(boris, anna)).isFalse();
    }

    @Test
    @DisplayName("Встречный лайк находится по обратной паре")
    void findsReciprocalLike() {
        long anna = testUsers.active();
        long boris = testUsers.active();
        likes.save(Like.from(anna, boris));

        likes.save(Like.from(boris, anna));

        assertThat(likes.existsBetween(anna, boris)).isTrue();
        assertThat(likes.existsBetween(boris, anna)).isTrue();
    }
}
