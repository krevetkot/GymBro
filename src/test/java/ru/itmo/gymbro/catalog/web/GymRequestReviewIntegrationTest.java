package ru.itmo.gymbro.catalog.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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
import ru.itmo.gymbro.profile.model.SportLevel;
import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// No surrounding test transaction: assertions observe real commits and rollbacks of HTTP operations.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class GymRequestReviewIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private UserRepository users;
    @Autowired
    private GymRepository gyms;
    @MockitoSpyBean
    private GymRequestRepository requests;
    @Autowired
    private UserProfileRepository profiles;
    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private TransactionTemplate transaction;
    private String city;
    private long authorId;
    private long adminId;
    private long requestId;
    private long existingGymId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        transaction = new TransactionTemplate(transactionManager);
        city = "Review-" + UUID.randomUUID();
        transaction.executeWithoutResult(status -> {
            authorId = users.save(User.register(city + "-user@example.com", "hash")).getId();
            adminId = users.save(new User(null, city + "-admin@example.com", "hash",
                    Role.ADMIN, UserStatus.ACTIVE, Instant.now())).getId();
            existingGymId = gyms.save(Gym.of("Old gym", city, "Old address")).getId();
            UserProfile profile = UserProfile.create(authorId, "Author", LocalDate.of(2000, 1, 1), "About");
            profile.replaceGyms(List.of(UserGym.of(existingGymId)));
            profile.addPhoto("https://example.com/photo.jpg");
            long sportId = jdbc.queryForObject("SELECT MIN(id) FROM sports", Long.class);
            profile.replaceSports(List.of(UserSport.of(sportId, SportLevel.BEGINNER)));
            profiles.save(profile);
            requestId = requests.save(GymRequest.submit(authorId, "New gym", city, "New address")).getId();
        });
    }

    @AfterEach
    void cleanUpCommittedFixtures() {
        transaction.executeWithoutResult(status -> {
            jdbc.update("DELETE FROM gym_requests WHERE author_id = ?", authorId);
            profiles.deleteByUserId(authorId);
            jdbc.update("DELETE FROM gyms WHERE city = ?", city);
            users.deleteById(authorId);
            users.deleteById(adminId);
        });
    }

    @Test
    void approvesWithoutLinkingGymToExistingProfile() throws Exception {
        Instant profileUpdatedAt = transaction.execute(status ->
                profiles.findByUserId(authorId).orElseThrow().getUpdatedAt());
        mvc.perform(post(action(requestId, "approve")).header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value(adminId))
                .andExpect(jsonPath("$.gymId").isNumber());

        transaction.executeWithoutResult(status -> {
            GymRequest request = requests.findById(requestId).orElseThrow();
            assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
            Gym created = gyms.findById(request.getGymId()).orElseThrow();
            assertThat(created.getCity()).isEqualTo(city);
            assertThat(created.getAddress()).isEqualTo("New address");
            UserProfile profile = profiles.findByUserId(authorId).orElseThrow();
            assertThat(profile.getGyms()).extracting(UserGym::getGymId)
                    .containsExactly(existingGymId);
            assertThat(profile.getUpdatedAt()).isEqualTo(profileUpdatedAt);
            assertThat(profile.getPhotos()).hasSize(1);
            assertThat(profile.getSports()).hasSize(1);
            assertThat(profile.getName()).isEqualTo("Author");
            assertThat(profile.getAbout()).isEqualTo("About");
        });
    }

    @Test
    void rejectsWithoutCreatingGymOrRequiringProfile() throws Exception {
        transaction.executeWithoutResult(status -> profiles.deleteByUserId(authorId));
        mvc.perform(post(action(requestId, "reject")).header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewedBy").value(adminId))
                .andExpect(jsonPath("$.gymId").doesNotExist());
        assertThat(requests.findById(requestId).orElseThrow().getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(newGymCount()).isZero();
    }

    @Test
    void approvesWithoutProfileAndDoesNotCreateOne() throws Exception {
        transaction.executeWithoutResult(status -> profiles.deleteByUserId(authorId));
        mvc.perform(post(action(requestId, "approve")).header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        assertThat(newGymCount()).isEqualTo(1);
        transaction.executeWithoutResult(status ->
                assertThat(profiles.findByUserId(authorId)).isEmpty());
    }

    @Test
    void rollsBackCreatedGymWhenSavingDecisionFails() throws Exception {
        doThrow(new DataAccessResourceFailureException("Injected decision save failure"))
                .when(requests).save(any(GymRequest.class));
        mvc.perform(post(action(requestId, "approve")).header("X-User-Id", adminId))
                .andExpect(status().isInternalServerError());

        // Gym INSERT was real; failure of the following save must roll it back.
        transaction.executeWithoutResult(status -> {
            GymRequest request = requests.findById(requestId).orElseThrow();
            assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(request.getReviewedBy()).isNull();
            assertThat(request.getGymId()).isNull();
            assertThat(newGymCount()).isZero();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    void rollsBackPersistedDecisionAndAllowsRetry(String operation) throws Exception {
        AtomicBoolean decisionWritten = new AtomicBoolean();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            String storedStatus = jdbc.queryForObject(
                    "SELECT status FROM gym_requests WHERE id = ?", String.class, requestId);
            assertThat(storedStatus).isEqualTo(operation.equals("approve") ? "APPROVED" : "REJECTED");
            decisionWritten.set(true);
            throw new DataAccessResourceFailureException("Injected failure after decision was written");
        }).when(requests).save(any(GymRequest.class));

        mvc.perform(post(action(requestId, operation)).header("X-User-Id", adminId))
                .andExpect(status().isInternalServerError());
        assertThat(decisionWritten).isTrue();

        transaction.executeWithoutResult(status -> {
            GymRequest stored = requests.findById(requestId).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(stored.getReviewedBy()).isNull();
            assertThat(stored.getGymId()).isNull();
            assertThat(newGymCount()).isZero();
            assertThat(profiles.findByUserId(authorId).orElseThrow().getGyms())
                    .extracting(UserGym::getGymId).containsExactly(existingGymId);
        });

        doCallRealMethod().when(requests).save(any(GymRequest.class));
        mvc.perform(post(action(requestId, operation)).header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(operation.equals("approve") ? "APPROVED" : "REJECTED"))
                .andExpect(jsonPath("$.reviewedBy").value(adminId));
        assertThat(newGymCount()).isEqualTo(operation.equals("approve") ? 1 : 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    void cannotReviewAnAlreadyReviewedRequest(String firstAction) throws Exception {
        mvc.perform(post(action(requestId, firstAction)).header("X-User-Id", adminId))
                .andExpect(status().isOk());
        for (String nextAction : List.of("approve", "reject")) {
            mvc.perform(post(action(requestId, nextAction)).header("X-User-Id", adminId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Заявка уже рассмотрена"));
        }
        assertThat(newGymCount()).isEqualTo(firstAction.equals("approve") ? 1 : 0);
    }

    @Test
    void conflictsIfGymAppearedAfterSubmission() throws Exception {
        transaction.executeWithoutResult(status -> gyms.save(Gym.of("Other gym", city, "New address")));
        mvc.perform(post(action(requestId, "approve")).header("X-User-Id", adminId))
                .andExpect(status().isConflict());
        assertThat(requests.findById(requestId).orElseThrow().getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(newGymCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    void checksAccessAndIdentifiers(String operation) throws Exception {
        mvc.perform(post(action(requestId, operation))).andExpect(status().isUnauthorized());
        mvc.perform(post(action(requestId, operation)).header("X-User-Id", authorId))
                .andExpect(status().isForbidden());
        mvc.perform(post(action(Long.MAX_VALUE, operation)).header("X-User-Id", adminId))
                .andExpect(status().isNotFound());
        mvc.perform(post(action(-1, operation)).header("X-User-Id", adminId))
                .andExpect(status().isBadRequest());
        transaction.executeWithoutResult(status -> {
            User admin = users.findById(adminId).orElseThrow();
            admin.ban();
            users.save(admin);
        });
        mvc.perform(post(action(requestId, operation)).header("X-User-Id", adminId))
                .andExpect(status().isForbidden());
        assertThat(requests.findById(requestId).orElseThrow().getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(newGymCount()).isZero();
    }

    @Test
    void concurrentApprovalsCreateOnlyOneGym() throws Exception {
        List<Integer> statuses = concurrently(
                () -> reviewStatus(requestId, "approve"),
                () -> reviewStatus(requestId, "approve"));
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(newGymCount()).isEqualTo(1);
        transaction.executeWithoutResult(status -> {
            GymRequest request = requests.findById(requestId).orElseThrow();
            assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
            assertThat(profiles.findByUserId(authorId).orElseThrow().getGyms())
                    .extracting(UserGym::getGymId).containsExactly(existingGymId);
        });
    }

    @Test
    void concurrentApprovalAndRejectionHaveOneWinner() throws Exception {
        assertThat(concurrently(
                () -> reviewStatus(requestId, "approve"),
                () -> reviewStatus(requestId, "reject")))
                .containsExactlyInAnyOrder(200, 409);
        GymRequest request = requests.findById(requestId).orElseThrow();
        assertThat(request.getStatus()).isIn(RequestStatus.APPROVED, RequestStatus.REJECTED);
        assertThat(newGymCount()).isEqualTo(request.getStatus() == RequestStatus.APPROVED ? 1 : 0);
    }

    @Test
    void concurrentDifferentApprovalsCreateCatalogEntriesWithoutChangingProfile() throws Exception {
        long otherId = transaction.execute(status ->
                requests.save(GymRequest.submit(authorId, "Second gym", city, "Second address")).getId());
        assertThat(concurrently(
                () -> reviewStatus(requestId, "approve"),
                () -> reviewStatus(otherId, "approve"))).containsOnly(200);
        transaction.executeWithoutResult(status -> {
            long firstGym = requests.findById(requestId).orElseThrow().getGymId();
            long secondGym = requests.findById(otherId).orElseThrow().getGymId();
            assertThat(firstGym).isNotEqualTo(secondGym);
            assertThat(gyms.findById(firstGym)).isPresent();
            assertThat(gyms.findById(secondGym)).isPresent();
            assertThat(profiles.findByUserId(authorId).orElseThrow().getGyms())
                    .extracting(UserGym::getGymId)
                    .containsExactly(existingGymId);
        });
    }

    private int reviewStatus(long id, String operation) throws Exception {
        return mvc.perform(post(action(id, operation)).header("X-User-Id", adminId))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void concurrentDifferentRequestsForSameAddressHaveOneWinner() throws Exception {
        long otherId = transaction.execute(status ->
                requests.save(GymRequest.submit(authorId, "Duplicate proposal", city, "New address")).getId());
        assertThat(concurrently(
                () -> reviewStatus(requestId, "approve"),
                () -> reviewStatus(otherId, "approve"))).containsExactlyInAnyOrder(200, 409);
        assertThat(newGymCount()).isEqualTo(1);
        transaction.executeWithoutResult(status -> {
            assertThat(List.of(requests.findById(requestId).orElseThrow().getStatus(),
                    requests.findById(otherId).orElseThrow().getStatus()))
                    .containsExactlyInAnyOrder(RequestStatus.APPROVED, RequestStatus.PENDING);
            assertThat(profiles.findByUserId(authorId).orElseThrow().getGyms())
                    .extracting(UserGym::getGymId).containsExactly(existingGymId);
        });
    }

    private List<Integer> concurrently(Callable<Integer> first, Callable<Integer> second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var tasks = List.of(first, second).stream().map(task -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Parallel test did not start");
                }
                return task.call();
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(tasks.get(0).get(20, TimeUnit.SECONDS), tasks.get(1).get(20, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private long newGymCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM gyms WHERE city = ? AND address = ?",
                Long.class, city, "New address");
    }

    private static String action(long id, String operation) {
        return "/api/v1/gym-requests/" + id + "/" + operation;
    }
}
