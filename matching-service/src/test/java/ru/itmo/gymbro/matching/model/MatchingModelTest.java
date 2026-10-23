package ru.itmo.gymbro.matching.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchingModelTest {

    @Test
    @DisplayName("Лайки равны, если совпадают отправитель и получатель, а id и время не важны")
    void likesAreEqualByDirectedPair() {
        Like first = new Like(1L, 2, 3, Instant.parse("2026-01-01T00:00:00Z"));
        Like same = new Like(9L, 2, 3, Instant.parse("2026-02-01T00:00:00Z"));
        Like reverse = Like.from(3, 2);

        assertThat(first).isEqualTo(same).hasSameHashCodeAs(same).isNotEqualTo(reverse);
        assertThat(first).isEqualTo(first).isNotEqualTo(null).isNotEqualTo("Like");
        assertThat(first.toString()).contains("from=2", "to=3");
    }

    @Test
    @DisplayName("Лайк нельзя поставить самому себе или без пользователей")
    void likeRejectsInvalidUsers() {
        assertThatThrownBy(() -> Like.from(2, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Like.from(0, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new Like(null, 2, 3, null).getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Мэтч хранит пару по возрастанию и равен мэтчу с той же парой")
    void matchesAreEqualByOrderedPair() {
        Match match = Match.between(7, 3);
        Match same = new Match(5L, 3, 7, null);

        assertThat(match.getUser1Id()).isEqualTo(3);
        assertThat(match.getUser2Id()).isEqualTo(7);
        assertThat(match).isEqualTo(same).hasSameHashCodeAs(same).isNotEqualTo(Match.between(3, 8));
        assertThat(match).isEqualTo(match).isNotEqualTo(null).isNotEqualTo("Match");
        assertThat(same.toString()).contains("id=5", "user1=3", "user2=7");
    }

    @Test
    @DisplayName("Партнёр по мэтчу — второй участник пары")
    void findsPartner() {
        Match match = Match.between(3, 7);

        assertThat(match.partnerOf(3)).isEqualTo(7);
        assertThat(match.partnerOf(7)).isEqualTo(3);
        assertThatThrownBy(() -> match.partnerOf(5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Мэтч нельзя создать с самим собой, без пользователей или с неупорядоченной парой")
    void matchRejectsInvalidPairs() {
        assertThatThrownBy(() -> Match.between(4, 4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Match(null, 0, 4, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Match(null, 5, 4, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
