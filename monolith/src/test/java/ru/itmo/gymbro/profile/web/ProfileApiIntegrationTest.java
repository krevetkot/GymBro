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
import ru.itmo.gymbro.catalog.model.Sport;
import ru.itmo.gymbro.catalog.repository.SportRepository;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.profile.model.SportLevel;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileApiIntegrationTest extends AbstractIntegrationTest {

    private static final String MY_PROFILE = "/api/v1/profiles/me";
    private static final LocalDate BIRTH_DATE = LocalDate.of(2003, 5, 17);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    @Autowired
    private UserProfileRepository profiles;

    @Autowired
    private SportRepository sports;

    private MockMvc mvc;
    private long userId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        userId = users.save(User.register("profile-owner@mail.ru", "hash")).getId();
    }

    @Test
    @DisplayName("Первый PUT создаёт анкету текущего пользователя — 201 с адресом анкеты")
    void createsProfileOnFirstPut() throws Exception {
        saveMine(userId, "Ксения", "2003-05-17", "бегаю по утрам")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/profiles/" + userId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.name").value("Ксения"))
                .andExpect(jsonPath("$.age").value(Period.between(BIRTH_DATE, LocalDate.now()).getYears()))
                .andExpect(jsonPath("$.about").value("бегаю по утрам"))
                .andExpect(jsonPath("$.sports", hasSize(0)))
                .andExpect(jsonPath("$.gymIds", hasSize(0)))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertThat(profiles.existsByUserId(userId)).isTrue();
    }

    @Test
    @DisplayName("Повторный PUT редактирует анкету — 200, запись остаётся одна")
    void editsExistingProfile() throws Exception {
        saveMine(userId, "Ксения", "2003-05-17", "бегаю").andExpect(status().isCreated());
        long profileId = profiles.findByUserId(userId).orElseThrow().getId();

        saveMine(userId, "Ксюша", "2002-01-01", null)
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.name").value("Ксюша"))
                .andExpect(jsonPath("$.about").doesNotExist());

        UserProfile stored = profiles.findByUserId(userId).orElseThrow();
        assertThat(stored.getId()).isEqualTo(profileId);
        assertThat(stored.getBirthDate()).isEqualTo(LocalDate.of(2002, 1, 1));
    }

    @Test
    @DisplayName("Редактирование не трогает виды спорта и залы анкеты")
    void keepsCollectionsOnEdit() throws Exception {
        long sportId = sports.save(Sport.of("Гребля на байдарках")).getId();
        UserProfile profile = UserProfile.create(userId, "Ксения", BIRTH_DATE, null);
        profile.replaceSports(Set.of(UserSport.of(sportId, SportLevel.ADVANCED)));
        profiles.save(profile);

        saveMine(userId, "Ксюша", "2003-05-17", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sports", hasSize(1)))
                .andExpect(jsonPath("$.sports[0].sportId").value(sportId))
                .andExpect(jsonPath("$.sports[0].level").value("ADVANCED"));
    }

    @Test
    @DisplayName("Анкета читается по id пользователя без заголовка X-User-Id")
    void readsProfileByUserId() throws Exception {
        profiles.save(UserProfile.create(userId, "Ксения", BIRTH_DATE, "плаваю"));

        mvc.perform(get("/api/v1/profiles/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.name").value("Ксения"))
                .andExpect(jsonPath("$.birthDate").doesNotExist());
    }

    @Test
    @DisplayName("Анкеты нет — 404")
    void returnsNotFoundWithoutProfile() throws Exception {
        mvc.perform(get("/api/v1/profiles/" + userId)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1"})
    @DisplayName("Идентификатор в пути не положительное число — 400")
    void rejectsInvalidIdentifier(String id) throws Exception {
        mvc.perform(get("/api/v1/profiles/" + id)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Без заголовка X-User-Id редактировать нельзя — 401")
    void requiresCurrentUser() throws Exception {
        mvc.perform(put(MY_PROFILE).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Ксения", "2003-05-17", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Несуществующий текущий пользователь — 401")
    void rejectsUnknownCurrentUser() throws Exception {
        saveMine(Long.MAX_VALUE, "Ксения", "2003-05-17", null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Заблокированный пользователь не может редактировать анкету — 403")
    void rejectsBannedUser() throws Exception {
        User user = users.findById(userId).orElseThrow();
        user.ban();
        users.save(user);

        saveMine(userId, "Ксения", "2003-05-17", null).andExpect(status().isForbidden());
        assertThat(profiles.existsByUserId(userId)).isFalse();
    }

    @Test
    @DisplayName("Невалидная анкета — 400 со списком полей, ничего не сохраняется")
    void rejectsInvalidProfile() throws Exception {
        saveMine(userId, " ", LocalDate.now().plusDays(1).toString(), "я".repeat(1001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "birthDate", "about")));

        assertThat(profiles.existsByUserId(userId)).isFalse();
    }

    @Test
    @DisplayName("Дата рождения в неверном формате — 400")
    void rejectsMalformedBirthDate() throws Exception {
        saveMine(userId, "Ксения", "17.05.2003", null).andExpect(status().isBadRequest());
    }

    private ResultActions saveMine(long currentUserId, String name, String birthDate, String about) throws Exception {
        return mvc.perform(put(MY_PROFILE)
                .header("X-User-Id", currentUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(name, birthDate, about)));
    }

    private static String body(String name, String birthDate, String about) {
        String aboutJson = about == null ? "null" : "\"" + about + "\"";
        return "{\"name\":\"%s\",\"birthDate\":\"%s\",\"about\":%s}".formatted(name, birthDate, aboutJson);
    }
}
