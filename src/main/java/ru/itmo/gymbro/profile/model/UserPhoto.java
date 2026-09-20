package ru.itmo.gymbro.profile.model;

import java.util.Objects;

public class UserPhoto {

    public static final int MAX_POSITION = 9;

    private Long id;
    private String url;
    private int position;

    public UserPhoto(Long id, String url, int position) {
        this.id = id;
        this.url = checkUrl(url);
        this.position = checkPosition(position);
    }

    public static UserPhoto at(String url, int position) {
        return new UserPhoto(null, url, position);
    }

    public void moveTo(int newPosition) {
        this.position = checkPosition(newPosition);
    }

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public int getPosition() {
        return position;
    }

    private static String checkUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ссылка на фотографию обязательна");
        }
        return value.trim();
    }

    private static int checkPosition(int value) {
        if (value < 0 || value > MAX_POSITION) {
            throw new IllegalArgumentException("Позиция фотографии должна быть от 0 до " + MAX_POSITION);
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserPhoto photo)) {
            return false;
        }
        return position == photo.position;
    }

    @Override
    public int hashCode() {
        return Objects.hash(position);
    }

    @Override
    public String toString() {
        return "UserPhoto{id=" + id + ", position=" + position + "}";
    }
}
