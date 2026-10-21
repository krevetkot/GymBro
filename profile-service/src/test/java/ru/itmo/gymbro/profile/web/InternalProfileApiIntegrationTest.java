package ru.itmo.gymbro.profile.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.profile.model.SportLevel;
import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    private UserProfileRepository profiles;

    private MockMvc mvc;
    private long sport;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        sport = testCatalog.sport();
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
    @DisplayName("Кандидаты отсортированы по числу общих видов спорта и залов со смотрящим")
    void ordersByOverlap() throws Exception {
        long otherSport = testCatalog.sport();
        long gym = testCatalog.gym();
        long viewer = userWithProfile("viewer", List.of(sport, otherSport), List.of(gym));
        long noOverlap = userWithProfile("none", List.of(), List.of());
        long partialMatch = userWithProfile("partial", List.of(sport), List.of());
        long bestMatch = userWithProfile("best", List.of(sport, otherSport), List.of(gym));

        String json = mvc.perform(get(FEED).param("viewerUserId", String.valueOf(viewer)).param("size", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Long> order = JsonPath.<List<Number>>read(json, "$.items[*].userId").stream()
                .map(Number::longValue).toList();
        assertThat(order).containsSubsequence(bestMatch, partialMatch, noOverlap);
    }

    @Test
    @DisplayName("Без анкеты смотрящего — 404")
    void requiresViewerProfile() throws Exception {
        long withoutProfile = testUsers.active();

        mvc.perform(get(FEED).param("viewerUserId", String.valueOf(withoutProfile)))
                .andExpect(status().isNotFound());
    }

    private long userWithProfile(String name) {
        return userWithProfile(name, List.of(sport), List.of());
    }

    private long userWithProfile(String name, List<Long> sportIds, List<Long> gymIds) {
        long userId = testUsers.active();
        UserProfile profile = UserProfile.create(userId, name, LocalDate.of(2000, 1, 1), null);
        profile.replaceSports(sportIds.stream().map(id -> UserSport.of(id, SportLevel.BEGINNER)).toList());
        profile.replaceGyms(gymIds.stream().map(UserGym::of).toList());
        profiles.save(profile);
        return userId;
    }
}
