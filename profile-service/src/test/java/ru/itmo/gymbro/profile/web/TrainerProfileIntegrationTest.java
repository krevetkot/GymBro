package ru.itmo.gymbro.profile.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.TestTokens;
import ru.itmo.gymbro.identity.api.Role;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тренер указывает цену часа, а в его анкете видно, что сертификат подтверждён.
 */
class TrainerProfileIntegrationTest extends AbstractIntegrationTest {

    private static final String HOURLY_RATE = "/api/v1/profiles/me/hourly-rate";
    private static final LocalDate BIRTH_DATE = LocalDate.of(1995, 3, 14);

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserProfileRepository profiles;

    private MockMvc mvc;
    private long trainerId;
    private long viewerId;

    @BeforeEach
    void setUp() {
        mvc = securedMvc(context);
        trainerId = testUsers.trainer();
        viewerId = testUsers.active();
        profiles.save(UserProfile.create(trainerId, "Тренер", BIRTH_DATE, "Ставлю технику"));
    }

    @Test
    @DisplayName("Тренер указывает цену часа; в его анкете видно цену и подтверждённый сертификат")
    void trainerSetsHourlyRate() throws Exception {
        setRate(trainerId, "1500")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainer.certificateVerified").value(true))
                .andExpect(jsonPath("$.trainer.hourlyRate").value(1500));

        viewProfile(trainerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainer.certificateVerified").value(true))
                .andExpect(jsonPath("$.trainer.hourlyRate").value(1500));
        assertThat(profiles.findByUserId(trainerId).orElseThrow().getHourlyRate()).isEqualTo(1500);
    }

    @Test
    @DisplayName("Тренер без цены: сертификат подтверждён, цены нет; null убирает цену")
    void trainerWithoutRate() throws Exception {
        viewProfile(trainerId)
                .andExpect(jsonPath("$.trainer.certificateVerified").value(true))
                .andExpect(jsonPath("$.trainer.hourlyRate").doesNotExist());

        setRate(trainerId, "2000").andExpect(status().isOk());
        setRate(trainerId, "null")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainer.hourlyRate").doesNotExist());
        assertThat(profiles.findByUserId(trainerId).orElseThrow().getHourlyRate()).isNull();
    }

    @Test
    @DisplayName("Обычный пользователь цену не указывает — 403, блока тренера в анкете нет")
    void userCannotSetRate() throws Exception {
        profiles.save(UserProfile.create(viewerId, "Пользователь", BIRTH_DATE, null));

        setRate(viewerId, "1000").andExpect(status().isForbidden());

        viewProfile(viewerId).andExpect(status().isOk()).andExpect(jsonPath("$.trainer").doesNotExist());
        assertThat(profiles.findByUserId(viewerId).orElseThrow().getHourlyRate()).isNull();
    }

    @Test
    @DisplayName("Тренер, с которого сняли роль, теряет блок тренера и не меняет цену даже со старым токеном")
    void demotedTrainer() throws Exception {
        setRate(trainerId, "1500").andExpect(status().isOk());
        String oldTrainerToken = TestTokens.bearer(trainerId);
        testUsers.changeRole(trainerId, Role.USER);

        viewProfile(trainerId).andExpect(jsonPath("$.trainer").doesNotExist());
        mvc.perform(put(HOURLY_RATE).header(HttpHeaders.AUTHORIZATION, oldTrainerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"hourlyRate\":900}"))
                .andExpect(status().isForbidden());
        assertThat(profiles.findByUserId(trainerId).orElseThrow().getHourlyRate()).isEqualTo(1500);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "100001", "\"дорого\""})
    @DisplayName("Цена вне диапазона 1..100000 — 400")
    void validatesRate(String rate) throws Exception {
        setRate(trainerId, rate).andExpect(status().isBadRequest());
        assertThat(profiles.findByUserId(trainerId).orElseThrow().getHourlyRate()).isNull();
    }

    @Test
    @DisplayName("Тренер без анкеты — 404, без токена — 401")
    void requiresProfileAndToken() throws Exception {
        setRate(testUsers.trainer(), "1500").andExpect(status().isNotFound());
        mvc.perform(put(HOURLY_RATE).contentType(MediaType.APPLICATION_JSON).content("{\"hourlyRate\":1500}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions setRate(long userId, String rate) throws Exception {
        return mvc.perform(put(HOURLY_RATE).header(HttpHeaders.AUTHORIZATION, TestTokens.bearer(userId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"hourlyRate\":" + rate + "}"));
    }

    private ResultActions viewProfile(long ownerId) throws Exception {
        return mvc.perform(get("/api/v1/profiles/" + ownerId).header(HttpHeaders.AUTHORIZATION, TestTokens.bearer(viewerId)));
    }
}
