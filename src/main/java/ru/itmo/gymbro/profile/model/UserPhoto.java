package ru.itmo.gymbro.profile.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

@Table("user_photos")
public class UserPhoto {

    @Id
    private Long id;
    private String url;

    public UserPhoto(Long id, String url) {
        this.id = id;
        this.url = checkUrl(url);
    }

    public static UserPhoto of(String url) {
        return new UserPhoto(null, url);
    }

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    private static String checkUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ссылка на фотографию обязательна");
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserPhoto photo)) {
            return false;
        }
        return url.equals(photo.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "UserPhoto{id=" + id + ", url=" + url + "}";
    }
}
