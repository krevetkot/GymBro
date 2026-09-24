package ru.itmo.gymbro.catalog.web;

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

import java.util.stream.Stream;

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

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private GymRepository gyms;

    @Autowired
    private ru.itmo.gymbro.identity.repository.UserRepository users;

    private long adminId;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        adminId = users.save(new ru.itmo.gymbro.identity.model.User(null, "catalog-admin@example.com", "hash",
                ru.itmo.gymbro.identity.model.Role.ADMIN, ru.itmo.gymbro.identity.model.UserStatus.ACTIVE,
                java.time.Instant.now())).getId();
    }

    static Stream<Arguments> catalogs() {
        return Stream.of(
                Arguments.of("/api/v1/sports",
                        "{\"name\":\"Test sport\"}",
                        "{\"name\":\"Updated sport\"}", "Updated sport"),
                Arguments.of("/api/v1/gyms",
                        "{\"name\":\"Test gym\",\"city\":\"Moscow\",\"address\":\"Street 1\"}",
                        "{\"name\":\"Updated gym\",\"city\":\"Kazan\",\"address\":\"Street 2\"}", "Updated gym"));
    }

    @ParameterizedTest
    @MethodSource("catalogs")
    void createsReadsUpdatesAndDeletes(String path, String body, String updated, String updatedName) throws Exception {
        String location = create(path, body);
        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());

        mvc.perform(put(location).header("X-User-Id", adminId).contentType(MediaType.APPLICATION_JSON).content(updated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updatedName));
        mvc.perform(get(location)).andExpect(jsonPath("$.name").value(updatedName));
        if (path.endsWith("gyms")) {
            mvc.perform(get(location))
                    .andExpect(jsonPath("$.city").value("Kazan"))
                    .andExpect(jsonPath("$.address").value("Street 2"));
        }

        // Updating with the same unique fields must not conflict with itself.
        mvc.perform(put(location).header("X-User-Id", adminId).contentType(MediaType.APPLICATION_JSON).content(updated))
                .andExpect(status().isOk());

        mvc.perform(delete(location).header("X-User-Id", adminId)).andExpect(status().isNoContent());
        mvc.perform(get(location)).andExpect(status().isNotFound());
        mvc.perform(delete(location).header("X-User-Id", adminId)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource("catalogs")
    void rejectsDuplicateCreation(String path, String body, String updated, String updatedName) throws Exception {
        create(path, body);
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @ParameterizedTest
    @MethodSource("catalogs")
    void rejectsUpdateToAnotherRecordsUniqueFields(
            String path, String body, String updated, String updatedName) throws Exception {
        String location = create(path, body);
        create(path, updated);
        mvc.perform(put(location).header("X-User-Id", adminId).contentType(MediaType.APPLICATION_JSON).content(updated))
                .andExpect(status().isConflict());
        // A conflict must leave the original resource intact.
        mvc.perform(get(location)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(path.endsWith("sports") ? "Test sport" : "Test gym"));
    }

    @ParameterizedTest
    @MethodSource("catalogs")
    void rejectsInvalidAndMalformedBodies(String path, String body, String updated, String updatedName) throws Exception {
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("Test", "x".repeat(301))))
                .andExpect(status().isBadRequest());
        String location = create(path, body);
        mvc.perform(put(location).header("X-User-Id", adminId).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("catalogs")
    void handlesMissingRecordsAndInvalidParameters(
            String path, String body, String updated, String updatedName) throws Exception {
        String missing = path + "/9223372036854775807";
        mvc.perform(get(missing)).andExpect(status().isNotFound());
        mvc.perform(put(missing).header("X-User-Id", adminId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        mvc.perform(delete(missing).header("X-User-Id", adminId)).andExpect(status().isNotFound());
        mvc.perform(get(path + "/-1")).andExpect(status().isBadRequest());
        mvc.perform(get(path + "/abc")).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("sort", "unknown,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("unknown")));
    }

    @Test
    void paginatesGymsWithTotalCountAndCapsSize() throws Exception {
        for (int i = 0; i < 55; i++) {
            gyms.save(Gym.of("Same name", "Test city", "Address " + i));
        }
        mvc.perform(get("/api/v1/gyms"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "55"))
                .andExpect(jsonPath("$", hasSize(20)));
        mvc.perform(get("/api/v1/gyms").param("size", "1000").param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "55"))
                .andExpect(jsonPath("$", hasSize(50)));
        mvc.perform(get("/api/v1/gyms").param("size", "50").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "55"))
                .andExpect(jsonPath("$", hasSize(5)));
        mvc.perform(get("/api/v1/gyms").param("page", "100"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "55"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listsSeededSportsWithTotalCount() throws Exception {
        mvc.perform(get("/api/v1/sports").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "10"))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void normalizesWhitespaceBeforeCheckingDuplicates() throws Exception {
        create("/api/v1/sports", "{\"name\":\"  New sport  \"}");
        mvc.perform(post("/api/v1/sports").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New sport\"}"))
                .andExpect(status().isConflict());
        create("/api/v1/gyms", "{\"name\":\" Gym \",\"city\":\" City \",\"address\":\" Address \"}");
        mvc.perform(post("/api/v1/gyms").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Another gym\",\"city\":\"City\",\"address\":\"Address\"}"))
                .andExpect(status().isConflict());
    }

    private String create(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(path + "/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getHeader("Location");
    }
}
