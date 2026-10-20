package ru.itmo.gymbro.profile.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Sport;
import ru.itmo.gymbro.catalog.repository.SportRepository;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.profile.model.SportLevel;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalProfileApiIntegrationTest extends AbstractIntegrationTest {

    private static final String FEED = "/internal/v1/profiles/feed";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    @Autowired
    private UserProfileRepository profiles;

    @Autowired
    private SportRepository sports;

    private MockMvc mvc;
    private String marker;
    private long sport;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        marker = UUID.randomUUID().toString().substring(0, 8);
        sport = sports.save(Sport.of("Спорт " + marker)).getId();
    }

    @Test
    @DisplayName("Кандидаты в ленту — снапшоты анкет без себя и исключённых")
    void returnsCandidatesWithoutExcluded() throws Exception {
        long viewer = userWithProfile("viewer");
        long candidate = userWithProfile("candidate");
        long excluded = userWithProfile("excluded");

        mvc.perform(get(FEED).param("viewerUserId", String.valueOf(viewer))
                        .param("excludedUserIds", String.valueOf(excluded))
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].userId", hasItem((int) candidate)))
                .andExpect(jsonPath("$.items[*].userId", not(hasItem((int) viewer))))
                .andExpect(jsonPath("$.items[*].userId", not(hasItem((int) excluded))))
                .andExpect(jsonPath("$.items[?(@.userId == " + candidate + ")].sportIds[0]", hasItem((int) sport)))
                .andExpect(jsonPath("$.hasNext").isBoolean());
    }

    @Test
    @DisplayName("Без анкеты смотрящего — 404")
    void requiresViewerProfile() throws Exception {
        long withoutProfile = users.save(User.register(marker + "-lonely@mail.ru", "hash")).getId();

        mvc.perform(get(FEED).param("viewerUserId", String.valueOf(withoutProfile)))
                .andExpect(status().isNotFound());
    }

    private long userWithProfile(String name) {
        long userId = users.save(User.register(marker + "-" + name + "@mail.ru", "hash")).getId();
        UserProfile profile = UserProfile.create(userId, name, LocalDate.of(2000, 1, 1), null);
        profile.replaceSports(List.of(UserSport.of(sport, SportLevel.BEGINNER)));
        profiles.save(profile);
        return userId;
    }
}
