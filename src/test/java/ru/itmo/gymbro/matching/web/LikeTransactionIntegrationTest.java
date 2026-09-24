package ru.itmo.gymbro.matching.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LikeTransactionIntegrationTest extends AbstractIntegrationTest {

    private static final int PAIRS = 20;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private final List<Long> createdUsers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void cleanUp() {
        createdUsers.forEach(users::deleteById);
        createdUsers.clear();
    }

    @Test
    @DisplayName("Одновременные встречные лайки создают ровно один мэтч в каждой паре")
    void concurrentMutualLikesCreateExactlyOneMatch() throws Exception {
        for (int pair = 0; pair < PAIRS; pair++) {
            long first = newUser();
            long second = newUser();

            List<MockHttpServletResponse> responses = concurrently(
                    () -> like(first, second),
                    () -> like(second, first));

            assertThat(responses).extracting(MockHttpServletResponse::getStatus).containsOnly(201);
            assertThat(responses).filteredOn(response -> matched(response)).hasSize(1);
            assertThat(matchesBetween(first, second)).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Одновременный повторный лайк сохраняется один раз, второй получает 409")
    void concurrentDuplicateLikesAreStoredOnce() throws Exception {
        long from = newUser();
        long to = newUser();

        List<MockHttpServletResponse> responses = concurrently(
                () -> like(from, to),
                () -> like(from, to));

        assertThat(responses).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM likes WHERE from_user_id = ? AND to_user_id = ?",
                Long.class, from, to)).isEqualTo(1);
    }

    private long newUser() {
        long id = users.save(User.register(UUID.randomUUID() + "@mail.ru", "hash")).getId();
        createdUsers.add(id);
        return id;
    }

    private MockHttpServletResponse like(long from, long to) throws Exception {
        return mvc.perform(post("/api/v1/likes")
                        .header("X-User-Id", from)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUserId\":%d}".formatted(to)))
                .andReturn().getResponse();
    }

    private static boolean matched(MockHttpServletResponse response) {
        try {
            return JsonPath.read(response.getContentAsString(), "$.matched");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private long matchesBetween(long one, long another) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM matches WHERE user1_id = ? AND user2_id = ?",
                Long.class, Math.min(one, another), Math.max(one, another));
    }

    private static <T> List<T> concurrently(Callable<T> first, Callable<T> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> results = new ArrayList<>();
            for (Callable<T> task : List.of(first, second)) {
                results.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<T> values = new ArrayList<>();
            for (Future<T> result : results) {
                values.add(result.get(20, TimeUnit.SECONDS));
            }
            return values;
        } finally {
            executor.shutdownNow();
        }
    }
}
