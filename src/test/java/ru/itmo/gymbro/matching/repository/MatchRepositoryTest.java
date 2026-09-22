package ru.itmo.gymbro.matching.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.matching.model.Match;

import static org.assertj.core.api.Assertions.assertThat;

class MatchRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private MatchRepository matches;

    @Autowired
    private UserRepository users;

    @Test
    @DisplayName("Пара упорядочивается по возрастанию независимо от порядка аргументов")
    void ordersPairAscending() {
        long anna = newUser("anna-match@mail.ru");
        long boris = newUser("boris-match@mail.ru");

        Match saved = matches.save(Match.between(Math.max(anna, boris), Math.min(anna, boris)));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser1Id()).isLessThan(saved.getUser2Id());
    }

    @Test
    @DisplayName("Мэтч находится по паре в любом порядке")
    void findsMatchByPairInAnyOrder() {
        long anna = newUser("anna-find@mail.ru");
        long boris = newUser("boris-find@mail.ru");
        matches.save(Match.between(anna, boris));

        assertThat(matches.findByPair(anna, boris)).isPresent();
        assertThat(matches.findByPair(boris, anna)).isPresent();
    }

    @Test
    @DisplayName("Мэтч знает второго участника пары")
    void knowsPartner() {
        long anna = newUser("anna-partner@mail.ru");
        long boris = newUser("boris-partner@mail.ru");
        Match saved = matches.save(Match.between(anna, boris));

        Match reloaded = matches.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.involves(anna)).isTrue();
        assertThat(reloaded.partnerOf(anna)).isEqualTo(boris);
        assertThat(reloaded.partnerOf(boris)).isEqualTo(anna);
    }

    private long newUser(String email) {
        return users.save(User.register(email, "hash")).getId();
    }
}
