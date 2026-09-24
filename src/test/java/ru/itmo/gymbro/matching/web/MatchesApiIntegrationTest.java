package ru.itmo.gymbro.matching.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.matching.model.Match;
import ru.itmo.gymbro.matching.repository.MatchRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MatchesApiIntegrationTest extends AbstractIntegrationTest {

    private static final String MATCHES = "/api/v1/matches";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    @Autowired
    private MatchRepository matches;

    private MockMvc mvc;
    private long viewer;
    private long older;
    private long newer;
    private long stranger;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        viewer = newUser();
        older = newUser();
        newer = newUser();
        stranger = newUser();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        matches.save(pair(viewer, older, now.minus(2, ChronoUnit.DAYS)));
        matches.save(pair(newer, viewer, now.minus(1, ChronoUnit.HOURS)));
        matches.save(pair(older, stranger, now));
    }

    @Test
    @DisplayName("Список мэтчей: только свои, партнёр с любой стороны пары, сначала новые")
    void listsOwnMatchesNewestFirst() throws Exception {
        mvc.perform(get(MATCHES).header("X-User-Id", viewer))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].partnerUserId", contains((int) newer, (int) older)))
                .andExpect(jsonPath("$[0].matchId").isNumber())
                .andExpect(jsonPath("$[0].matchedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Страницы: размер из запроса, общее количество в заголовке")
    void pagesMatches() throws Exception {
        mvc.perform(get(MATCHES).header("X-User-Id", viewer).param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].partnerUserId").value(older));
    }

    @Test
    @DisplayName("Сортировка из запроса игнорируется, больше 50 записей не отдаётся")
    void ignoresClientSortAndCapsSize() throws Exception {
        mvc.perform(get(MATCHES).header("X-User-Id", viewer).param("sort", "user1Id,asc").param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].partnerUserId", contains((int) newer, (int) older)));
    }

    @Test
    @DisplayName("Нет мэтчей — пустой список и нулевое количество")
    void returnsEmptyListWithoutMatches() throws Exception {
        long lonely = newUser();

        mvc.perform(get(MATCHES).header("X-User-Id", lonely))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Без X-User-Id — 401, забаненный пользователь — 403")
    void checksCurrentUser() throws Exception {
        mvc.perform(get(MATCHES)).andExpect(status().isUnauthorized());

        User user = users.findById(viewer).orElseThrow();
        user.ban();
        users.save(user);

        mvc.perform(get(MATCHES).header("X-User-Id", viewer)).andExpect(status().isForbidden());
    }

    private long newUser() {
        return users.save(User.register(UUID.randomUUID() + "@mail.ru", "hash")).getId();
    }

    private static Match pair(long one, long another, Instant createdAt) {
        return new Match(null, Math.min(one, another), Math.max(one, another), createdAt);
    }
}
