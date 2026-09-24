package ru.itmo.gymbro.matching.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.matching.repository.LikeRepository;
import ru.itmo.gymbro.matching.repository.MatchRepository;
import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LikeApiIntegrationTest extends AbstractIntegrationTest {

    private static final String LIKES = "/api/v1/likes";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    @Autowired
    private UserProfileRepository profiles;

    @Autowired
    private GymRepository gyms;

    @Autowired
    private LikeRepository likes;

    @Autowired
    private MatchRepository matches;

    private MockMvc mvc;
    private long anna;
    private long boris;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        anna = newUser();
        boris = newUser();
    }

    @Test
    @DisplayName("Односторонний лайк — 201, мэтча нет")
    void oneSidedLikeCreatesNoMatch() throws Exception {
        like(anna, boris)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.toUserId").value(boris))
                .andExpect(jsonPath("$.likedAt").isNotEmpty())
                .andExpect(jsonPath("$.matched").value(false))
                .andExpect(jsonPath("$.matchId").doesNotExist());

        assertThat(likes.existsBetween(anna, boris)).isTrue();
        assertThat(matches.findByPair(anna, boris)).isEmpty();
    }

    @Test
    @DisplayName("Встречный лайк создаёт мэтч и возвращает его id")
    void mutualLikeCreatesMatch() throws Exception {
        like(anna, boris).andExpect(jsonPath("$.matched").value(false));

        String json = like(boris, anna)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matched").value(true))
                .andExpect(jsonPath("$.matchId").isNumber())
                .andReturn().getResponse().getContentAsString();

        long matchId = ((Number) JsonPath.read(json, "$.matchId")).longValue();
        assertThat(matches.findByPair(anna, boris)).get()
                .satisfies(match -> assertThat(match.getId()).isEqualTo(matchId));
    }

    @Test
    @DisplayName("Мэтч виден в списке у обоих участников")
    void matchIsListedForBothUsers() throws Exception {
        like(anna, boris);
        like(boris, anna);

        mvc.perform(get("/api/v1/matches").header("X-User-Id", anna))
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].partnerUserId").value(boris));
        mvc.perform(get("/api/v1/matches").header("X-User-Id", boris))
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].partnerUserId").value(anna));
    }

    @Test
    @DisplayName("Повторный лайк — 409, в базе один лайк")
    void repeatedLikeConflicts() throws Exception {
        like(anna, boris).andExpect(status().isCreated());

        like(anna, boris)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());

        assertThat(likes.findLikedUserIds(anna)).containsExactly(boris);
    }

    @Test
    @DisplayName("Повторный лайк после мэтча — 409, второй мэтч не создаётся")
    void repeatedLikeAfterMatchConflicts() throws Exception {
        like(anna, boris);
        like(boris, anna);

        like(boris, anna).andExpect(status().isConflict());

        mvc.perform(get("/api/v1/matches").header("X-User-Id", anna))
                .andExpect(header().string("X-Total-Count", "1"));
    }

    @Test
    @DisplayName("Лайк самому себе — 400, ничего не сохраняется")
    void selfLikeIsRejected() throws Exception {
        like(anna, anna)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());

        assertThat(likes.findLikedUserIds(anna)).isEmpty();
    }

    @Test
    @DisplayName("Лайк несуществующему пользователю — 404")
    void likeOfUnknownUserIsNotFound() throws Exception {
        like(anna, Long.MAX_VALUE).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Лайк забаненному пользователю — 404, ничего не сохраняется")
    void likeOfBannedUserIsNotFound() throws Exception {
        ban(boris);

        like(anna, boris).andExpect(status().isNotFound());

        assertThat(likes.existsBetween(anna, boris)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"toUserId\":0}", "{\"toUserId\":-5}"})
    @DisplayName("Невалидное тело — 400 с полем toUserId")
    void invalidBodyIsRejected(String body) throws Exception {
        mvc.perform(post(LIKES).header("X-User-Id", anna).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("toUserId"));
    }

    @Test
    @DisplayName("Нечисловой toUserId — 400")
    void nonNumericTargetIsRejected() throws Exception {
        mvc.perform(post(LIKES).header("X-User-Id", anna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUserId\":\"abc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Без X-User-Id — 401, забаненный лайкающий — 403")
    void checksCurrentUser() throws Exception {
        mvc.perform(post(LIKES).contentType(MediaType.APPLICATION_JSON).content("{\"toUserId\":%d}".formatted(boris)))
                .andExpect(status().isUnauthorized());

        ban(anna);

        like(anna, boris).andExpect(status().isForbidden());
        assertThat(likes.existsBetween(anna, boris)).isFalse();
    }

    @Test
    @DisplayName("Лайкнутая анкета пропадает из ленты")
    void likedProfileLeavesFeed() throws Exception {
        long gymId = gyms.save(Gym.of("Общий зал", UUID.randomUUID().toString(), "Ленина, 1")).getId();
        withProfileInGym(anna, gymId);
        withProfileInGym(boris, gymId);
        assertThat(feedUserIds(anna)).contains(boris);

        like(anna, boris);

        assertThat(feedUserIds(anna)).doesNotContain(boris);
    }

    private ResultActions like(long from, long to) throws Exception {
        return mvc.perform(post(LIKES)
                .header("X-User-Id", from)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"toUserId\":%d}".formatted(to)));
    }

    private long newUser() {
        return users.save(User.register(UUID.randomUUID() + "@mail.ru", "hash")).getId();
    }

    private void ban(long userId) {
        User user = users.findById(userId).orElseThrow();
        user.ban();
        users.save(user);
    }

    private void withProfileInGym(long userId, long gymId) {
        UserProfile profile = UserProfile.create(userId, "user" + userId, LocalDate.of(2000, 1, 1), null);
        profile.replaceGyms(List.of(UserGym.of(gymId)));
        profiles.save(profile);
    }

    private List<Long> feedUserIds(long viewerId) throws Exception {
        String json = mvc.perform(get("/api/v1/feed").header("X-User-Id", viewerId).param("size", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(json, "$.items[*].userId");
        return ids.stream().map(Number::longValue).toList();
    }
}
