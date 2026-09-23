package ru.itmo.gymbro.catalog.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.model.GymRequest;
import ru.itmo.gymbro.catalog.model.RequestStatus;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.GymRequestRepository;
import ru.itmo.gymbro.identity.model.Role;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.model.UserStatus;
import ru.itmo.gymbro.identity.repository.UserRepository;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GymRequestApiIntegrationTest extends AbstractIntegrationTest {

    private static final String PATH = "/api/v1/gym-requests";
    private static final String BODY = "{\"name\":\" New gym \",\"city\":\" City \",\"address\":\" Address \"}";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository users;
    @Autowired
    private GymRequestRepository requests;
    @Autowired
    private GymRepository gyms;
    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private long authorId;
    private long adminId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        authorId = users.save(User.register("request-author@example.com", "hash")).getId();
        adminId = users.save(new User(null, "request-admin@example.com", "hash",
                Role.ADMIN, UserStatus.ACTIVE, Instant.now())).getId();
    }

    @Test
    void submitsPendingRequestAndPersistsStringStatusWithoutCreatingGym() throws Exception {
        mvc.perform(post(PATH).header("X-User-Id", authorId)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.authorId").value(authorId))
                .andExpect(jsonPath("$.name").value("New gym"))
                .andExpect(jsonPath("$.city").value("City"))
                .andExpect(jsonPath("$.address").value("Address"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.gymId").doesNotExist())
                .andExpect(jsonPath("$.reviewedBy").doesNotExist());

        var saved = requests.findAll(PageRequest.of(0, 20));
        assertThat(saved.getTotalElements()).isEqualTo(1);
        assertThat(saved.getContent().getFirst().getAuthorId()).isEqualTo(authorId);
        assertThat(jdbc.queryForObject("SELECT status FROM gym_requests", String.class)).isEqualTo("PENDING");
        assertThat(gyms.findAll(PageRequest.of(0, 20)).getTotalElements()).isZero();
    }

    @Test
    void requiresCurrentUserForBothEndpoints() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        assertThat(requests.findAll(PageRequest.of(0, 20)).getTotalElements()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "-1", "0", "999999999999999999"})
    void rejectsInvalidOrUnknownCurrentUser(String userId) throws Exception {
        mvc.perform(post(PATH).header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("X-User-Id", userId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deniesListingToOrdinaryUser() throws Exception {
        requests.save(GymRequest.submit(authorId, "Gym", "City", "Address"));
        mvc.perform(get(PATH).header("X-User-Id", authorId))
                .andExpect(status().isForbidden());
    }

    @Test
    void deniesBannedUserAndBannedAdmin() throws Exception {
        User author = users.findById(authorId).orElseThrow();
        author.ban();
        users.save(author);
        User admin = users.findById(adminId).orElseThrow();
        admin.ban();
        users.save(admin);

        mvc.perform(post(PATH).header("X-User-Id", authorId)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(get(PATH).header("X-User-Id", adminId))
                .andExpect(status().isForbidden());
        assertThat(requests.findAll(PageRequest.of(0, 20)).getTotalElements()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"name\":\"Gym\",\"city\":\" \",\"address\":\"Address\"}"})
    void rejectsInvalidSubmission(String body) throws Exception {
        mvc.perform(post(PATH).header("X-User-Id", authorId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(requests.findAll(PageRequest.of(0, 20)).getTotalElements()).isZero();
    }

    @Test
    void rejectsOversizedFields() throws Exception {
        mvc.perform(post(PATH).header("X-User-Id", authorId)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("New gym", "x".repeat(201))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsRequestForExistingGym() throws Exception {
        gyms.save(Gym.of("Existing gym", "City", "Address"));
        mvc.perform(post(PATH).header("X-User-Id", authorId)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());
        assertThat(requests.findAll(PageRequest.of(0, 20)).getTotalElements()).isZero();
    }

    @Test
    void paginatesForAdminWithTotalCountAndDeterministicNewestFirstOrder() throws Exception {
        long lastId = 0;
        Instant sameTime = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < 55; i++) {
            lastId = requests.save(new GymRequest(null, authorId, "Gym", "City", "Address " + i,
                    RequestStatus.PENDING, null, null, sameTime)).getId();
        }
        mvc.perform(get(PATH).header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "55"))
                .andExpect(jsonPath("$", hasSize(20)))
                .andExpect(jsonPath("$[0].id").value(lastId));
        mvc.perform(get(PATH).header("X-User-Id", adminId).param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "55"))
                .andExpect(jsonPath("$", hasSize(50)));
        mvc.perform(get(PATH).header("X-User-Id", adminId).param("size", "50").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "55"))
                .andExpect(jsonPath("$", hasSize(5)));
        mvc.perform(get(PATH).header("X-User-Id", adminId).param("page", "100"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "55"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void returnsEmptyListAndRejectsUnknownSortForAdmin() throws Exception {
        mvc.perform(get(PATH).header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(PATH).header("X-User-Id", adminId).param("sort", "unknown,asc"))
                .andExpect(status().isBadRequest());
    }
}

