package ru.itmo.gymbro.profile.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.profile.model.SportLevel;
import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProfileAggregateIntegrationTest extends AbstractIntegrationTest {

    private static final String MY_PROFILE = "/api/v1/profiles/me";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    @Autowired
    private UserProfileRepository profiles;

    @Autowired
    private GymRepository gyms;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private String marker;
    private long userId;
    private long gymId;
    private List<Long> sportIds;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        marker = UUID.randomUUID().toString();
        userId = users.save(User.register(marker + "@mail.ru", "hash")).getId();
        gymId = gyms.save(Gym.of("Зал", marker, "Ленина, 1")).getId();
        sportIds = jdbc.queryForList("SELECT id FROM sports ORDER BY id LIMIT 3", Long.class);
    }

    @AfterEach
    void cleanUp() {
        profiles.deleteByUserId(userId);
        users.deleteById(userId);
        gyms.deleteById(gymId);
    }

    @Test
    @DisplayName("Замена видов спорта не оставляет старых строк в user_sports")
    void replacingSportsLeavesNoOrphanRows() throws Exception {
        createProfile();
        perform(put(MY_PROFILE + "/sports"), sportsBody(sportIds.get(0), sportIds.get(1)));
        perform(put(MY_PROFILE + "/sports"), sportsBody(sportIds.get(2)));

        assertThat(jdbc.queryForList("SELECT sport_id FROM user_sports WHERE profile_id = ?", Long.class, profileId()))
                .containsExactly(sportIds.get(2));
    }

    @Test
    @DisplayName("Пустой список залов удаляет все строки user_gyms анкеты")
    void clearingGymsDeletesRows() throws Exception {
        createProfile();
        perform(put(MY_PROFILE + "/gyms"), "{\"gymIds\":[%d]}".formatted(gymId));
        perform(put(MY_PROFILE + "/gyms"), "{\"gymIds\":[]}");

        assertThat(countRows("user_gyms")).isZero();
    }

    @Test
    @DisplayName("Редактирование анкеты не дублирует и не теряет дочерние строки")
    void editingProfileKeepsChildRows() throws Exception {
        createProfile();
        perform(put(MY_PROFILE + "/sports"), sportsBody(sportIds.get(0), sportIds.get(1)));
        perform(put(MY_PROFILE + "/gyms"), "{\"gymIds\":[%d]}".formatted(gymId));

        perform(put(MY_PROFILE), profileBody("Новое имя"));

        assertThat(countRows("user_sports")).isEqualTo(2);
        assertThat(countRows("user_gyms")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_profiles WHERE user_id = ?", Long.class, userId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Сбой при сохранении откатывает агрегат целиком: корень, залы и виды спорта")
    void failedSaveRollsBackWholeAggregate() {
        UserProfile profile = UserProfile.create(userId, "Ксения", LocalDate.of(2003, 5, 17), null);
        profile.replaceGyms(List.of(UserGym.of(gymId)));
        profile.replaceSports(List.of(UserSport.of(sportIds.get(0), SportLevel.BEGINNER)));
        profiles.save(profile);

        UserProfile changed = profiles.findByUserId(userId).orElseThrow();
        changed.edit("Другое имя", LocalDate.of(2003, 5, 17), null);
        changed.replaceGyms(List.of());
        changed.replaceSports(List.of(UserSport.of(Long.MAX_VALUE, SportLevel.ADVANCED)));

        assertThatThrownBy(() -> profiles.save(changed)).isInstanceOf(DataIntegrityViolationException.class);

        UserProfile stored = profiles.findByUserId(userId).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Ксения");
        assertThat(stored.getGyms()).extracting(UserGym::getGymId).containsExactly(gymId);
        assertThat(stored.getSports()).extracting(UserSport::getSportId).containsExactly(sportIds.get(0));
    }

    @Test
    @DisplayName("Два одновременных первых PUT создают одну анкету")
    void concurrentFirstPutsCreateOneProfile() throws Exception {
        List<Integer> statuses = concurrently(
                () -> status(put(MY_PROFILE), profileBody("Первая")),
                () -> status(put(MY_PROFILE), profileBody("Вторая")));

        assertThat(statuses).contains(201).containsAnyOf(200, 409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_profiles WHERE user_id = ?", Long.class, userId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Одновременные замены видов спорта оставляют один из списков целиком, без смеси")
    void concurrentSportReplacementsDoNotMix() throws Exception {
        createProfile();

        List<Integer> statuses = concurrently(
                () -> status(put(MY_PROFILE + "/sports"), sportsBody(sportIds.get(0), sportIds.get(1))),
                () -> status(put(MY_PROFILE + "/sports"), sportsBody(sportIds.get(2))));

        assertThat(statuses).containsOnly(200);
        Set<Long> stored = Set.copyOf(
                jdbc.queryForList("SELECT sport_id FROM user_sports WHERE profile_id = ?", Long.class, profileId()));
        assertThat(stored).isIn(Set.of(sportIds.get(0), sportIds.get(1)), Set.of(sportIds.get(2)));
    }

    @Test
    @DisplayName("Одновременные изменения видов спорта и залов не затирают друг друга")
    void concurrentChangesOfDifferentCollectionsAreNotLost() throws Exception {
        createProfile();

        List<Integer> statuses = concurrently(
                () -> status(put(MY_PROFILE + "/sports"), sportsBody(sportIds.get(0))),
                () -> status(put(MY_PROFILE + "/gyms"), "{\"gymIds\":[%d]}".formatted(gymId)));

        assertThat(statuses).containsOnly(200);
        assertThat(jdbc.queryForList("SELECT sport_id FROM user_sports WHERE profile_id = ?", Long.class, profileId()))
                .containsExactly(sportIds.get(0));
        assertThat(jdbc.queryForList("SELECT gym_id FROM user_gyms WHERE profile_id = ?", Long.class, profileId()))
                .containsExactly(gymId);
    }

    private void createProfile() throws Exception {
        perform(put(MY_PROFILE), profileBody("Ксения"));
    }

    private void perform(MockHttpServletRequestBuilder request,
                         String body) throws Exception {
        int code = status(request, body);
        assertThat(code).isBetween(200, 299);
    }

    private int status(MockHttpServletRequestBuilder request,
                       String body) throws Exception {
        request.header("X-User-Id", userId);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    private long profileId() {
        return profiles.findByUserId(userId).orElseThrow().getId();
    }

    private long countRows(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE profile_id = ?", Long.class, profileId());
    }

    private static String profileBody(String name) {
        return "{\"name\":\"%s\",\"birthDate\":\"2003-05-17\"}".formatted(name);
    }

    private static String sportsBody(Long... ids) {
        List<String> items = new ArrayList<>();
        for (Long id : ids) {
            items.add("{\"sportId\":%d,\"level\":\"BEGINNER\"}".formatted(id));
        }
        return "{\"sports\":[" + String.join(",", items) + "]}";
    }

    private static List<Integer> concurrently(Callable<Integer> first, Callable<Integer> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (Callable<Integer> task : List.of(first, second)) {
                results.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get(20, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            executor.shutdownNow();
        }
    }
}
