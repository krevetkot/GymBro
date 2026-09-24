package ru.itmo.gymbro.profile.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.itmo.gymbro.AbstractIntegrationTest;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.profile.model.UserPhoto;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.repository.UserProfileRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfilePhotoApiIntegrationTest extends AbstractIntegrationTest {

    private static final String MY_PHOTOS = "/api/v1/profiles/me/photos";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    @Autowired
    private UserProfileRepository profiles;

    private MockMvc mvc;
    private long userId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        userId = users.save(User.register("photographer@mail.ru", "hash")).getId();
        profiles.save(UserProfile.create(userId, "Ксения", LocalDate.of(2003, 5, 17), null));
    }

    @Test
    @DisplayName("Фото добавляется в конец анкеты — 201, позиции идут по порядку добавления")
    void addsPhotosInOrder() throws Exception {
        addPhoto(userId, "https://cdn/first.jpg")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/profiles/" + userId))
                .andExpect(jsonPath("$.photos", hasSize(1)));

        addPhoto(userId, "https://cdn/second.jpg")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photos[*].position", contains(0, 1)))
                .andExpect(jsonPath("$.photos[*].url", contains("https://cdn/first.jpg", "https://cdn/second.jpg")));
    }

    @Test
    @DisplayName("Удаление фото — 204, оставшиеся сдвигаются без дыр в позициях")
    void removesPhotoAndShiftsPositions() throws Exception {
        addPhoto(userId, "https://cdn/a.jpg");
        addPhoto(userId, "https://cdn/b.jpg");
        addPhoto(userId, "https://cdn/c.jpg");

        mvc.perform(delete(MY_PHOTOS + "/1").header("X-User-Id", userId))
                .andExpect(status().isNoContent());

        UserProfile stored = profiles.findByUserId(userId).orElseThrow();
        assertThat(stored.getPhotos()).extracting(UserPhoto::getUrl)
                .containsExactly("https://cdn/a.jpg", "https://cdn/c.jpg");
    }

    @Test
    @DisplayName("Позиции без фото — 404")
    void rejectsMissingPosition() throws Exception {
        addPhoto(userId, "https://cdn/only.jpg");

        mvc.perform(delete(MY_PHOTOS + "/1").header("X-User-Id", userId))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "abc"})
    @DisplayName("Позиция не неотрицательное число — 400")
    void rejectsInvalidPosition(String position) throws Exception {
        mvc.perform(delete(MY_PHOTOS + "/" + position).header("X-User-Id", userId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Одиннадцатое фото — 409, в анкете остаётся десять")
    void rejectsPhotoOverLimit() throws Exception {
        for (int index = 0; index < UserProfile.MAX_PHOTOS; index++) {
            addPhoto(userId, "https://cdn/" + index + ".jpg").andExpect(status().isCreated());
        }

        addPhoto(userId, "https://cdn/extra.jpg").andExpect(status().isConflict());

        assertThat(profiles.findByUserId(userId).orElseThrow().getPhotos()).hasSize(UserProfile.MAX_PHOTOS);
    }

    @Test
    @DisplayName("Та же ссылка второй раз — 400")
    void rejectsDuplicatePhoto() throws Exception {
        addPhoto(userId, "https://cdn/same.jpg").andExpect(status().isCreated());

        addPhoto(userId, "https://cdn/same.jpg").andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "ftp://cdn/photo.jpg", "not a link", "https://cdn/with space.jpg"})
    @DisplayName("Невалидная ссылка — 400 с полем url")
    void rejectsInvalidUrl(String url) throws Exception {
        addPhoto(userId, url)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("url"));
    }

    @Test
    @DisplayName("Ссылка длиннее 500 символов — 400")
    void rejectsTooLongUrl() throws Exception {
        addPhoto(userId, "https://cdn/" + "a".repeat(500)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Без анкеты фото не добавить — 404")
    void requiresExistingProfile() throws Exception {
        long withoutProfile = users.save(User.register("no-photos@mail.ru", "hash")).getId();

        addPhoto(withoutProfile, "https://cdn/photo.jpg").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Без X-User-Id — 401, забаненный пользователь — 403")
    void checksCurrentUser() throws Exception {
        mvc.perform(post(MY_PHOTOS).contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://cdn/x.jpg\"}"))
                .andExpect(status().isUnauthorized());

        User user = users.findById(userId).orElseThrow();
        user.ban();
        users.save(user);

        addPhoto(userId, "https://cdn/x.jpg").andExpect(status().isForbidden());
        mvc.perform(delete(MY_PHOTOS + "/0").header("X-User-Id", userId)).andExpect(status().isForbidden());
    }

    private ResultActions addPhoto(long currentUserId, String url) throws Exception {
        return mvc.perform(post(MY_PHOTOS)
                .header("X-User-Id", currentUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"%s\"}".formatted(url)));
    }
}
