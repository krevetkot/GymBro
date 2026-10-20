package ru.itmo.gymbro.catalog.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.identity.model.Role;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.model.UserStatus;
import ru.itmo.gymbro.identity.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogApiIntegrationTest extends AbstractIntegrationTest {

    private static final String SPORTS = "/api/v1/sports";
    private static final String GYMS = "/api/v1/gyms";
    private static final String GYM = "{\"name\":\"Test gym\",\"city\":\"Moscow\",\"address\":\"Street 1\"}";
    private static final String UPDATED_GYM = "{\"name\":\"Updated gym\",\"city\":\"Kazan\",\"address\":\"Street 2\"}";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private GymRepository gyms;

    @Autowired
    private UserRepository users;

    private long adminId;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        adminId = users.save(new User(null, "catalog-admin@example.com", "hash",
                Role.ADMIN, UserStatus.ACTIVE, Instant.now())).getId();
    }

    static Stream<Arguments> catalogs() {
        return Stream.of(
                Arguments.of(SPORTS, "{\"name\":\"Test sport\"}"),
                Arguments.of(GYMS, GYM));
    }

    @Test
    void createsListsAndDeletesSport() throws Exception {
        long id = create(SPORTS, "{\"name\":\"Test sport\"}");

        String page = mvc.perform(get(SPORTS).param("size", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(page, "$[*].id");
        assertThat(ids).extracting(Number::longValue).contains(id);

        mvc.perform(delete(SPORTS + "/" + id).header("X-User-Id", adminId)).andExpect(status().isNoContent());
        mvc.perform(delete(SPORTS + "/" + id).header("X-User-Id", adminId)).andExpect(status().isNotFound());
    }

    @Test
    void createsReadsUpdatesAndDeletesGym() throws Exception {
        String location = mvc.perform(post(GYMS).header("X-User-Id", adminId)
                        .contentType(MediaType.APPLICATION_JSON).content(GYM))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(GYMS + "/")))
                .andReturn().getResponse().getHeader("Location");
        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());

        mvc.perform(put(location).header("X-User-Id", adminId)
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATED_GYM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated gym"));
        mvc.perform(get(location))
                .andExpect(jsonPath("$.name").value("Updated gym"))
                .andExpect(jsonPath("$.city").value("Kazan"))
                .andExpect(jsonPath("$.address").value("Street 2"));

        mvc.perform(put(location).header("X-User-Id", adminId)
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATED_GYM))
                .andExpect(status().isOk());

        mvc.perform(delete(location).header("X-User-Id", adminId)).andExpect(status().isNoContent());
        mvc.perform(get(location)).andExpect(status().isNotFound());
        mvc.perform(delete(location).header("X-User-Id", adminId)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource("catalogs")
    void rejectsDuplicateCreation(String path, String body) throws Exception {
        create(path, body);
        mvc.perform(post(path).header("X-User-Id", adminId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void rejectsGymUpdateToAnotherRecordsUniqueFields() throws Exception {
        long id = create(GYMS, GYM);
        create(GYMS, UPDATED_GYM);
        mvc.perform(put(GYMS + "/" + id).header("X-User-Id", adminId)
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATED_GYM))
                .andExpect(status().isConflict());
        mvc.perform(get(GYMS + "/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test gym"));
    }

    @ParameterizedTest
    @MethodSource("catalogs")
    void rejectsInvalidAndMalformedBodies(String path, String body) throws Exception {
        mvc.perform(post(path).header("X-User-Id", adminId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
        mvc.perform(post(path).header("X-User-Id", adminId).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(path).header("X-User-Id", adminId).contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("Test", "x".repeat(301))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidGymUpdate() throws Exception {
        long id = create(GYMS, GYM);
        mvc.perform(put(GYMS + "/" + id).header("X-User-Id", adminId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("catalogs")
    void handlesMissingRecordsAndInvalidParameters(String path, String body) throws Exception {
        mvc.perform(delete(path + "/9223372036854775807").header("X-User-Id", adminId))
                .andExpect(status().isNotFound());
        mvc.perform(delete(path + "/-1").header("X-User-Id", adminId)).andExpect(status().isBadRequest());
        mvc.perform(delete(path + "/abc").header("X-User-Id", adminId)).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("sort", "unknown,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("unknown")));
    }

    @Test
    void handlesMissingGymOnReadAndUpdate() throws Exception {
        String missing = GYMS + "/9223372036854775807";
        mvc.perform(get(missing)).andExpect(status().isNotFound());
        mvc.perform(put(missing).header("X-User-Id", adminId).contentType(MediaType.APPLICATION_JSON).content(GYM))
                .andExpect(status().isNotFound());
        mvc.perform(get(GYMS + "/-1")).andExpect(status().isBadRequest());
        mvc.perform(get(GYMS + "/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void paginatesGymsWithTotalCountAndCapsSize() throws Exception {
        for (int i = 0; i < 55; i++) {
            gyms.save(Gym.of("Same name", "Test city", "Address " + i));
        }
        mvc.perform(get(GYMS))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "55"))
                .andExpect(jsonPath("$", hasSize(20)));
        mvc.perform(get(GYMS).param("size", "1000").param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "55"))
                .andExpect(jsonPath("$", hasSize(50)));
        mvc.perform(get(GYMS).param("size", "50").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "55"))
                .andExpect(jsonPath("$", hasSize(5)));
        mvc.perform(get(GYMS).param("page", "100"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "55"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listsSeededSportsWithTotalCount() throws Exception {
        mvc.perform(get(SPORTS).param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "10"))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void normalizesWhitespaceBeforeCheckingDuplicates() throws Exception {
        create(SPORTS, "{\"name\":\"  New sport  \"}");
        mvc.perform(post(SPORTS).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"New sport\"}"))
                .andExpect(status().isConflict());
        create(GYMS, "{\"name\":\" Gym \",\"city\":\" City \",\"address\":\" Address \"}");
        mvc.perform(post(GYMS).header("X-User-Id", adminId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Another gym\",\"city\":\"City\",\"address\":\"Address\"}"))
                .andExpect(status().isConflict());
    }

    private long create(String path, String body) throws Exception {
        String json = mvc.perform(post(path).header("X-User-Id", adminId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
