package ru.itmo.gymbro.matching.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.matching.model.Like;
import ru.itmo.gymbro.matching.repository.LikeRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedApiIntegrationTest extends AbstractIntegrationTest {

    private static final String FEED = "/api/v1/feed";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private LikeRepository likes;

    private MockMvc mvc;
    private long viewer;
    private long candidate;
    private long likedMe;
    private long banned;
    private long alreadyLiked;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        viewer = userWithProfile();
        candidate = userWithProfile();
        likedMe = userWithProfile();
        banned = userWithProfile();
        alreadyLiked = userWithProfile();

        testUsers.ban(banned);
        likes.save(Like.from(viewer, alreadyLiked));
        likes.save(Like.from(likedMe, viewer));
    }

    @Test
    @DisplayName("В ленте нет себя, забаненных и уже лайкнутых; лайкнувшие меня остаются")
    void excludesSelfBannedAndLiked() throws Exception {
        List<Long> shown = scrollWholeFeed(viewer);

        assertThat(shown).contains(candidate, likedMe);
        assertThat(shown).doesNotContain(viewer, banned, alreadyLiked);
        assertThat(shown).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Ответ — срез без общего количества: items, page, size, hasNext")
    void returnsSliceWithoutTotal() throws Exception {
        mvc.perform(get(FEED).header("X-User-Id", viewer).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Total-Count"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.total").doesNotExist())
                .andExpect(jsonPath("$.totalElements").doesNotExist());
    }

    @Test
    @DisplayName("Карточка заполняется из анкеты, которую отдал profile-service")
    void fillsCard() throws Exception {
        long withDetails = testUsers.active();
        testProfiles.add(withDetails, List.of(3L, 5L), List.of(7L));

        String json = mvc.perform(get(FEED).header("X-User-Id", viewer).param("size", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String card = "$.items[?(@.userId == " + withDetails + ")]";
        assertThat(JsonPath.<List<String>>read(json, card + ".name")).containsExactly("user" + withDetails);
        assertThat(JsonPath.<List<Integer>>read(json, card + ".age")).containsExactly(25);
        assertThat(JsonPath.<List<String>>read(json, card + ".about")).containsExactly("О себе");
        assertThat(JsonPath.<List<List<Integer>>>read(json, card + ".sportIds")).containsExactly(List.of(3, 5));
        assertThat(JsonPath.<List<List<Integer>>>read(json, card + ".gymIds")).containsExactly(List.of(7));
    }

    @Test
    @DisplayName("Больше 50 карточек за запрос не отдаётся")
    void capsPageSize() throws Exception {
        mvc.perform(get(FEED).header("X-User-Id", viewer).param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    @DisplayName("Без своей анкеты ленты нет — 404")
    void requiresViewerProfile() throws Exception {
        long withoutProfile = testUsers.active();

        mvc.perform(get(FEED).header("X-User-Id", withoutProfile)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Без X-User-Id — 401, забаненный смотрящий — 403")
    void checksViewer() throws Exception {
        mvc.perform(get(FEED)).andExpect(status().isUnauthorized());
        mvc.perform(get(FEED).header("X-User-Id", banned)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Страницы ленты идут подряд без пропусков")
    void pagesFollowEachOther() throws Exception {
        List<Long> firstFour = new ArrayList<>(page(viewer, 0, 2));
        firstFour.addAll(page(viewer, 1, 2));

        assertThat(firstFour).hasSize(4).doesNotHaveDuplicates();
        assertThat(page(viewer, 0, 50)).startsWith(firstFour.toArray(Long[]::new));
    }

    private long userWithProfile() {
        long userId = testUsers.active();
        testProfiles.add(userId);
        return userId;
    }

    private List<Long> page(long viewerId, int page, int size) throws Exception {
        String json = mvc.perform(get(FEED).header("X-User-Id", viewerId)
                        .param("size", String.valueOf(size)).param("page", String.valueOf(page)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(json, "$.items[*].userId");
        return ids.stream().map(Number::longValue).toList();
    }

    private List<Long> scrollWholeFeed(long viewerId) throws Exception {
        List<Long> shown = new ArrayList<>();
        int page = 0;
        boolean hasNext = true;
        while (hasNext) {
            String json = mvc.perform(get(FEED).header("X-User-Id", viewerId)
                            .param("size", "50").param("page", String.valueOf(page)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<Number> ids = JsonPath.read(json, "$.items[*].userId");
            ids.forEach(id -> shown.add(id.longValue()));
            hasNext = JsonPath.read(json, "$.hasNext");
            page++;
        }
        return shown;
    }
}
