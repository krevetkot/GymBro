package ru.itmo.gymbro.catalog.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.model.GymRequest;
import ru.itmo.gymbro.catalog.model.RequestStatus;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.GymRequestRepository;

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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockingDetails;

class GymRequestReviewIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private GymRepository gyms;
    @MockitoSpyBean
    private GymRequestRepository requests;

    private String city;
    private long authorId;
    private long adminId;
    private long requestId;

    @BeforeEach
    void setUp() {
        city = "Review-" + UUID.randomUUID();
        authorId = testUsers.active();
        adminId = testUsers.admin();
        requestId = requests.save(GymRequest.submit(authorId, "New gym", city, "New address")).block().getId();
    }

    @Test
    void approvesAndCreatesGymFromRequest() {
        review(requestId, "approve")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("APPROVED")
                .jsonPath("$.reviewedBy").isEqualTo(adminId)
                .jsonPath("$.gymId").isNumber();

        GymRequest request = requests.findById(requestId).block();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        Gym created = gyms.findById(request.getGymId()).block();
        assertThat(created.getCity()).isEqualTo(city);
        assertThat(created.getAddress()).isEqualTo("New address");
    }

    @Test
    void rejectsWithoutCreatingGym() {
        review(requestId, "reject")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("REJECTED")
                .jsonPath("$.reviewedBy").isEqualTo(adminId)
                .jsonPath("$.gymId").doesNotExist();
        assertThat(requests.findById(requestId).block().getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(newGymCount()).isZero();
    }

    @Test
    void rollsBackCreatedGymWhenSavingDecisionFails() {
        doReturn(Mono.error(new DataAccessResourceFailureException("Injected decision save failure")))
                .when(requests).save(any(GymRequest.class));

        review(requestId, "approve").expectStatus().is5xxServerError();

        GymRequest request = requests.findById(requestId).block();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(request.getReviewedBy()).isNull();
        assertThat(request.getGymId()).isNull();
        assertThat(newGymCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    @SuppressWarnings("unchecked")
    void rollsBackPersistedDecisionAndAllowsRetry(String operation) {
        AtomicBoolean decisionWritten = new AtomicBoolean();
        Answer<?> realSave = mockingDetails(requests).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> ((Mono<GymRequest>) realSave.answer(invocation))
                .flatMap(saved -> database.sql("SELECT status FROM gym_requests WHERE id = :id")
                        .bind("id", requestId)
                        .map(row -> row.get("status", String.class))
                        .one())
                .doOnNext(storedStatus -> {
                    assertThat(storedStatus).isEqualTo(operation.equals("approve") ? "APPROVED" : "REJECTED");
                    decisionWritten.set(true);
                })
                .then(Mono.error(new DataAccessResourceFailureException("Injected failure after decision was written"))))
                .when(requests).save(any(GymRequest.class));

        review(requestId, operation).expectStatus().is5xxServerError();
        assertThat(decisionWritten).isTrue();

        GymRequest stored = requests.findById(requestId).block();
        assertThat(stored.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(stored.getReviewedBy()).isNull();
        assertThat(stored.getGymId()).isNull();
        assertThat(newGymCount()).isZero();

        doAnswer(realSave).when(requests).save(any(GymRequest.class));
        review(requestId, operation)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(operation.equals("approve") ? "APPROVED" : "REJECTED")
                .jsonPath("$.reviewedBy").isEqualTo(adminId);
        assertThat(newGymCount()).isEqualTo(operation.equals("approve") ? 1 : 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    void cannotReviewAnAlreadyReviewedRequest(String firstAction) {
        review(requestId, firstAction).expectStatus().isOk();
        for (String nextAction : List.of("approve", "reject")) {
            review(requestId, nextAction)
                    .expectStatus().isEqualTo(409)
                    .expectBody().jsonPath("$.detail").isEqualTo("Заявка уже рассмотрена");
        }
        assertThat(newGymCount()).isEqualTo(firstAction.equals("approve") ? 1 : 0);
    }

    @Test
    void conflictsIfGymAppearedAfterSubmission() {
        gyms.save(Gym.of("Other gym", city, "New address")).block();

        review(requestId, "approve").expectStatus().isEqualTo(409);
        assertThat(requests.findById(requestId).block().getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(newGymCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject"})
    void checksAccessAndIdentifiers(String operation) {
        client.post().uri(action(requestId, operation)).exchange().expectStatus().isUnauthorized();
        review(requestId, operation, authorId).expectStatus().isForbidden();
        review(Long.MAX_VALUE, operation).expectStatus().isNotFound();
        review(-1, operation).expectStatus().isBadRequest();
        testUsers.ban(adminId);
        review(requestId, operation).expectStatus().isForbidden();
        assertThat(requests.findById(requestId).block().getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(newGymCount()).isZero();
    }

    @Test
    void concurrentApprovalsCreateOnlyOneGym() throws Exception {
        List<Integer> statuses = concurrently(
                () -> reviewStatus(requestId, "approve"),
                () -> reviewStatus(requestId, "approve"));

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(newGymCount()).isEqualTo(1);
        assertThat(requests.findById(requestId).block().getStatus()).isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    void concurrentApprovalAndRejectionHaveOneWinner() throws Exception {
        assertThat(concurrently(
                () -> reviewStatus(requestId, "approve"),
                () -> reviewStatus(requestId, "reject")))
                .containsExactlyInAnyOrder(200, 409);
        GymRequest request = requests.findById(requestId).block();
        assertThat(request.getStatus()).isIn(RequestStatus.APPROVED, RequestStatus.REJECTED);
        assertThat(newGymCount()).isEqualTo(request.getStatus() == RequestStatus.APPROVED ? 1 : 0);
    }

    @Test
    void concurrentDifferentApprovalsCreateSeparateGyms() throws Exception {
        long otherId = requests.save(GymRequest.submit(authorId, "Second gym", city, "Second address"))
                .block().getId();

        assertThat(concurrently(
                () -> reviewStatus(requestId, "approve"),
                () -> reviewStatus(otherId, "approve"))).containsOnly(200);
        long firstGym = requests.findById(requestId).block().getGymId();
        long secondGym = requests.findById(otherId).block().getGymId();
        assertThat(firstGym).isNotEqualTo(secondGym);
        assertThat(gyms.existsById(firstGym).block()).isTrue();
        assertThat(gyms.existsById(secondGym).block()).isTrue();
    }

    @Test
    void concurrentDifferentRequestsForSameAddressHaveOneWinner() throws Exception {
        long otherId = requests.save(GymRequest.submit(authorId, "Duplicate proposal", city, "New address"))
                .block().getId();

        assertThat(concurrently(
                () -> reviewStatus(requestId, "approve"),
                () -> reviewStatus(otherId, "approve"))).containsExactlyInAnyOrder(200, 409);
        assertThat(newGymCount()).isEqualTo(1);
        assertThat(List.of(requests.findById(requestId).block().getStatus(),
                requests.findById(otherId).block().getStatus()))
                .containsExactlyInAnyOrder(RequestStatus.APPROVED, RequestStatus.PENDING);
    }

    private WebTestClient.ResponseSpec review(long id, String operation) {
        return review(id, operation, adminId);
    }

    private WebTestClient.ResponseSpec review(
            long id, String operation, long userId) {
        return client.post().uri(action(id, operation)).header("X-User-Id", String.valueOf(userId)).exchange();
    }

    private int reviewStatus(long id, String operation) {
        return review(id, operation).returnResult(Void.class).getStatus().value();
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
        return database.sql("SELECT COUNT(*) AS total FROM gyms WHERE city = :city AND address = :address")
                .bind("city", city)
                .bind("address", "New address")
                .map(row -> row.get("total", Long.class))
                .one()
                .block();
    }

    private static String action(long id, String operation) {
        return "/api/v1/gym-requests/" + id + "/" + operation;
    }
}
