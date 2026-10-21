package ru.itmo.gymbro.catalog.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.repository.GymRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GymAdminAccessIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private GymRepository gyms;
    private MockMvc mvc;
    private long gymId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        gymId = gyms.save(Gym.of("Original", "City", "Address")).getId();
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "delete"})
    void requiresAnExistingUser(String operation) throws Exception {
        mvc.perform(change(operation)).andExpect(status().isUnauthorized());
        mvc.perform(change(operation).header("X-User-Id", Long.MAX_VALUE))
                .andExpect(status().isUnauthorized());
        assertUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "delete"})
    void rejectsNonAdminsAndBannedAdminsWithoutChangingGym(String operation) throws Exception {
        long bannedAdmin = testUsers.admin();
        testUsers.ban(bannedAdmin);
        for (long userId : List.of(testUsers.active(), bannedAdmin)) {
            mvc.perform(change(operation).header("X-User-Id", userId))
                    .andExpect(status().isForbidden());
            assertUnchanged();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "delete"})
    void allowsActiveAdmin(String operation) throws Exception {
        long adminId = testUsers.admin();
        mvc.perform(change(operation).header("X-User-Id", adminId))
                .andExpect(status().is(operation.equals("update") ? 200 : 204));
        if (operation.equals("update")) {
            assertThat(gyms.findById(gymId).orElseThrow().getName()).isEqualTo("Updated");
        } else {
            assertThat(gyms.findById(gymId)).isEmpty();
        }
    }

    private MockHttpServletRequestBuilder change(String operation) {
        String path = "/api/v1/gyms/" + gymId;
        return operation.equals("delete") ? delete(path) : put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Updated","city":"Other city","address":"Other address"}
                        """);
    }

    private void assertUnchanged() {
        Gym stored = gyms.findById(gymId).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Original");
        assertThat(stored.getCity()).isEqualTo("City");
        assertThat(stored.getAddress()).isEqualTo("Address");
    }
}
