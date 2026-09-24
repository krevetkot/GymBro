package ru.itmo.gymbro.matching.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.model.Sport;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.SportRepository;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.matching.model.Like;
import ru.itmo.gymbro.matching.repository.LikeRepository;
import ru.itmo.gymbro.profile.model.SportLevel;
import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedApiIntegrationTest extends AbstractIntegrationTest {

    private static final String FEED = "/api/v1/feed";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    @Autowired
    private UserProfileRepository profiles;

    @Autowired
    private SportRepository sports;

    @Autowired
    private GymRepository gyms;

    @Autowired
    private LikeRepository likes;

    private MockMvc mvc;
    private String marker;
    private long sportA;
    private long sportB;
    private long gym;
    private long viewer;
    private long bestMatch;
    private long partialMatch;
    private long noOverlap;
    private long banned;
    private long alreadyLiked;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        marker = UUID.randomUUID().toString().substring(0, 8);
        sportA = sports.save(Sport.of("Спорт A " + marker)).getId();
        sportB = sports.save(Sport.of("Спорт B " + marker)).getId();
        gym = gyms.save(Gym.of("Зал", marker, "Ленина, 1")).getId();

        viewer = userWithProfile("viewer", List.of(sportA, sportB), List.of(gym));
        noOverlap = userWithProfile("none", List.of(), List.of());
        partialMatch = userWithProfile("partial", List.of(sportA), List.of());
        bestMatch = userWithProfile("best", List.of(sportA, sportB), List.of(gym));
        banned = userWithProfile("banned", List.of(sportA, sportB), List.of(gym));
        alreadyLiked = userWithProfile("liked", List.of(sportA, sportB), List.of(gym));

        User bannedUser = users.findById(banned).orElseThrow();
        bannedUser.ban();
        users.save(bannedUser);
        likes.save(Like.from(viewer, alreadyLiked));
        likes.save(Like.from(partialMatch, viewer));
    }

    @Test
    @DisplayName("Лента начинается с анкет с наибольшим пересечением видов спорта и залов")
    void ordersByOverlap() throws Exception {
        mvc.perform(get(FEED).header("X-User-Id", viewer).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].userId").value(bestMatch))
                .andExpect(jsonPath("$.items[1].userId").value(partialMatch));
    }

    @Test
    @DisplayName("В ленте нет себя, забаненных и уже лайкнутых; лайкнувшие меня остаются")
    void excludesSelfBannedAndLiked() throws Exception {
        List<Long> shown = scrollWholeFeed(viewer);

        assertThat(shown).contains(bestMatch, partialMatch, noOverlap);
        assertThat(shown).doesNotContain(viewer, banned, alreadyLiked);
        assertThat(shown).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Ответ — срез без общего количества: items, page, size, hasNext")
    void returnsSliceWithoutTotal() throws Exception {
        mvc.perform(get(FEED).header("X-User-Id", viewer).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Total-Count"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.total").doesNotExist())
                .andExpect(jsonPath("$.totalElements").doesNotExist());

        mvc.perform(get(FEED).header("X-User-Id", viewer).param("size", "1").param("page", "1"))
                .andExpect(jsonPath("$.items[0].userId").value(partialMatch));
    }

    @Test
    @DisplayName("Карточка содержит данные анкеты")
    void fillsCard() throws Exception {
        mvc.perform(get(FEED).header("X-User-Id", viewer).param("size", "1"))
                .andExpect(jsonPath("$.items[0].name").value("best"))
                .andExpect(jsonPath("$.items[0].age").isNumber())
                .andExpect(jsonPath("$.items[0].sportIds", containsInAnyOrder((int) sportA, (int) sportB)))
                .andExpect(jsonPath("$.items[0].gymIds[0]").value(gym));
    }

    @Test
    @DisplayName("Больше 50 карточек за запрос не отдаётся")
    void capsPageSize() throws Exception {
        mvc.perform(get(FEED).header("X-User-Id", viewer).param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    @DisplayName("Без своей анкеты ленты нет — 404")
    void requiresViewerProfile() throws Exception {
        long withoutProfile = users.save(User.register(marker + "-lonely@mail.ru", "hash")).getId();

        mvc.perform(get(FEED).header("X-User-Id", withoutProfile)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Без X-User-Id — 401, забаненный смотрящий — 403")
    void checksViewer() throws Exception {
        mvc.perform(get(FEED)).andExpect(status().isUnauthorized());
        mvc.perform(get(FEED).header("X-User-Id", banned)).andExpect(status().isForbidden());
    }

    private long userWithProfile(String name, List<Long> sportIds, List<Long> gymIds) {
        long userId = users.save(User.register(marker + "-" + name + "@mail.ru", "hash")).getId();
        UserProfile profile = UserProfile.create(userId, name, LocalDate.of(2000, 1, 1), null);
        profile.replaceSports(sportIds.stream().map(id -> UserSport.of(id, SportLevel.BEGINNER)).toList());
        profile.replaceGyms(gymIds.stream().map(UserGym::of).toList());
        profiles.save(profile);
        return userId;
    }

    private List<Long> scrollWholeFeed(long viewerId) throws Exception {
        List<Long> shown = new ArrayList<>();
        int page = 0;
        boolean hasNext = true;
        while (hasNext) {
            String json = mvc.perform(get(FEED).header("X-User-Id", viewerId)
                            .param("size", "50").param("page", String.valueOf(page)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<Number> ids = JsonPath.read(json, "$.items[*].userId");
            ids.forEach(id -> shown.add(id.longValue()));
            hasNext = JsonPath.read(json, "$.hasNext");
            page++;
        }
        return shown;
    }
}
