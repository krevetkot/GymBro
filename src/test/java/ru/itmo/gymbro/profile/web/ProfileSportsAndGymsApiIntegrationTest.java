package ru.itmo.gymbro.profile.web;

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
import ru.itmo.gymbro.catalog.model.Sport;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.SportRepository;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.profile.model.SportLevel;
import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.time.LocalDate;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileSportsAndGymsApiIntegrationTest extends AbstractIntegrationTest {

    private static final String MY_SPORTS = "/api/v1/profiles/me/sports";
    private static final String MY_GYMS = "/api/v1/profiles/me/gyms";

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

    private MockMvc mvc;
    private long userId;
    private long running;
    private long swimming;
    private long firstGym;
    private long secondGym;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        userId = users.save(User.register("athlete@mail.ru", "hash")).getId();
        profiles.save(UserProfile.create(userId, "Ксения", LocalDate.of(2003, 5, 17), null));
        running = sports.save(Sport.of("Ориентирование")).getId();
        swimming = sports.save(Sport.of("Водное поло")).getId();
        firstGym = gyms.save(Gym.of("Первый", "Москва", "Ленина, 1")).getId();
        secondGym = gyms.save(Gym.of("Второй", "Москва", "Ленина, 2")).getId();
    }

    @Test
    @DisplayName("Виды спорта с уровнем сохраняются в анкету")
    void replacesSports() throws Exception {
        putJson(userId, MY_SPORTS, sportsBody(running, "BEGINNER", swimming, "ADVANCED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sports", hasSize(2)))
                .andExpect(jsonPath("$.sports[?(@.sportId == %d)].level".formatted(running)).value("BEGINNER"))
                .andExpect(jsonPath("$.sports[?(@.sportId == %d)].level".formatted(swimming)).value("ADVANCED"));

        UserProfile stored = profiles.findByUserId(userId).orElseThrow();
        assertThat(stored.getSports()).extracting(UserSport::getSportId).containsExactlyInAnyOrder(running, swimming);
    }

    @Test
    @DisplayName("Новый список видов спорта заменяет старый целиком, уровень меняется")
    void overwritesPreviousSports() throws Exception {
        putJson(userId, MY_SPORTS, sportsBody(running, "BEGINNER", swimming, "ADVANCED")).andExpect(status().isOk());

        putJson(userId, MY_SPORTS, "{\"sports\":[{\"sportId\":%d,\"level\":\"INTERMEDIATE\"}]}".formatted(running))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sports", hasSize(1)))
                .andExpect(jsonPath("$.sports[0].level").value("INTERMEDIATE"));

        UserProfile stored = profiles.findByUserId(userId).orElseThrow();
        assertThat(stored.getSports()).extracting(UserSport::getLevel).containsExactly(SportLevel.INTERMEDIATE);
    }

    @Test
    @DisplayName("Пустой список очищает виды спорта")
    void clearsSports() throws Exception {
        putJson(userId, MY_SPORTS, sportsBody(running, "BEGINNER", swimming, "ADVANCED")).andExpect(status().isOk());

        putJson(userId, MY_SPORTS, "{\"sports\":[]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sports", hasSize(0)));
    }

    @Test
    @DisplayName("Несуществующий вид спорта — 404, анкета не меняется")
    void rejectsUnknownSport() throws Exception {
        putJson(userId, MY_SPORTS, sportsBody(running, "BEGINNER", Long.MAX_VALUE, "BEGINNER"))
                .andExpect(status().isNotFound());

        assertThat(profiles.findByUserId(userId).orElseThrow().getSports()).isEmpty();
    }

    @Test
    @DisplayName("Один вид спорта дважды — 400")
    void rejectsDuplicateSport() throws Exception {
        putJson(userId, MY_SPORTS, sportsBody(running, "BEGINNER", running, "ADVANCED"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"sports\":[null]}",
            "{\"sports\":[{\"sportId\":0,\"level\":\"BEGINNER\"}]}",
            "{\"sports\":[{\"sportId\":1}]}"})
    @DisplayName("Невалидный список видов спорта — 400 со списком полей")
    void rejectsInvalidSportsBody(String body) throws Exception {
        putJson(userId, MY_SPORTS, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Неизвестный уровень — 400")
    void rejectsUnknownLevel() throws Exception {
        putJson(userId, MY_SPORTS, "{\"sports\":[{\"sportId\":%d,\"level\":\"PRO\"}]}".formatted(running))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Больше 20 видов спорта — 400")
    void rejectsTooManySports() throws Exception {
        String items = String.join(",", IntStream.rangeClosed(1, 21)
                .mapToObj(id -> "{\"sportId\":%d,\"level\":\"BEGINNER\"}".formatted(id))
                .toList());

        putJson(userId, MY_SPORTS, "{\"sports\":[" + items + "]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sports"));
    }

    @Test
    @DisplayName("Залы сохраняются в анкету и заменяются целиком")
    void replacesGyms() throws Exception {
        putJson(userId, MY_GYMS, "{\"gymIds\":[%d,%d]}".formatted(secondGym, firstGym))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gymIds", contains((int) firstGym, (int) secondGym)));

        putJson(userId, MY_GYMS, "{\"gymIds\":[%d]}".formatted(secondGym))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gymIds", contains((int) secondGym)));

        UserProfile stored = profiles.findByUserId(userId).orElseThrow();
        assertThat(stored.getGyms()).extracting(UserGym::getGymId).containsExactly(secondGym);
    }

    @Test
    @DisplayName("Несуществующий зал — 404")
    void rejectsUnknownGym() throws Exception {
        putJson(userId, MY_GYMS, "{\"gymIds\":[%d,%d]}".formatted(firstGym, Long.MAX_VALUE))
                .andExpect(status().isNotFound());

        assertThat(profiles.findByUserId(userId).orElseThrow().getGyms()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"gymIds\":[null]}", "{\"gymIds\":[-1]}"})
    @DisplayName("Невалидный список залов — 400")
    void rejectsInvalidGymsBody(String body) throws Exception {
        putJson(userId, MY_GYMS, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Один зал дважды — 400")
    void rejectsDuplicateGym() throws Exception {
        putJson(userId, MY_GYMS, "{\"gymIds\":[%d,%d]}".formatted(firstGym, firstGym))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Без анкеты менять виды спорта и залы нельзя — 404")
    void requiresExistingProfile() throws Exception {
        long withoutProfile = users.save(User.register("no-profile@mail.ru", "hash")).getId();

        putJson(withoutProfile, MY_SPORTS, sportsBody(running, "BEGINNER", swimming, "ADVANCED"))
                .andExpect(status().isNotFound());
        putJson(withoutProfile, MY_GYMS, "{\"gymIds\":[%d]}".formatted(firstGym))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Без X-User-Id — 401, забаненный пользователь — 403")
    void checksCurrentUser() throws Exception {
        mvc.perform(put(MY_GYMS).contentType(MediaType.APPLICATION_JSON).content("{\"gymIds\":[]}"))
                .andExpect(status().isUnauthorized());

        User user = users.findById(userId).orElseThrow();
        user.ban();
        users.save(user);

        putJson(userId, MY_SPORTS, "{\"sports\":[]}").andExpect(status().isForbidden());
    }

    private ResultActions putJson(long currentUserId, String path, String body) throws Exception {
        return mvc.perform(put(path)
                .header("X-User-Id", currentUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String sportsBody(long firstId, String firstLevel, long secondId, String secondLevel) {
        return "{\"sports\":[{\"sportId\":%d,\"level\":\"%s\"},{\"sportId\":%d,\"level\":\"%s\"}]}"
                .formatted(firstId, firstLevel, secondId, secondLevel);
    }
}
