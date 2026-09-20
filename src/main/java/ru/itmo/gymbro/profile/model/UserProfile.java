package ru.itmo.gymbro.profile.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class UserProfile {

    public static final int MAX_PHOTOS = 10;

    private Long id;
    private long userId;
    private String name;
    private LocalDate birthDate;
    private String about;
    private List<UserPhoto> photos;
    private Set<UserSport> sports;
    private Set<UserGym> gyms;
    private Instant updatedAt;

    public UserProfile(Long id, long userId, String name, LocalDate birthDate, String about,
                       Collection<UserPhoto> photos, Collection<UserSport> sports,
                       Collection<UserGym> gyms, Instant updatedAt) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Профиль должен быть привязан к пользователю");
        }
        this.id = id;
        this.userId = userId;
        this.name = checkName(name);
        this.birthDate = checkBirthDate(birthDate);
        this.about = normalizeAbout(about);
        this.photos = checkPhotos(photos);
        this.sports = new LinkedHashSet<>(sports == null ? List.of() : sports);
        this.gyms = new LinkedHashSet<>(gyms == null ? List.of() : gyms);
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public static UserProfile create(long userId, String name, LocalDate birthDate, String about) {
        return new UserProfile(null, userId, name, birthDate, about,
                List.of(), Set.of(), Set.of(), Instant.now());
    }

    public void edit(String newName, LocalDate newBirthDate, String newAbout) {
        this.name = checkName(newName);
        this.birthDate = checkBirthDate(newBirthDate);
        this.about = normalizeAbout(newAbout);
        touch();
    }

    public void replaceSports(Collection<UserSport> newSports) {
        this.sports = new LinkedHashSet<>(newSports == null ? List.of() : newSports);
        touch();
    }

    public void replaceGyms(Collection<UserGym> newGyms) {
        this.gyms = new LinkedHashSet<>(newGyms == null ? List.of() : newGyms);
        touch();
    }

    public void addPhoto(String url) {
        if (photos.size() >= MAX_PHOTOS) {
            throw new IllegalStateException("Достигнут предел в " + MAX_PHOTOS + " фотографий");
        }
        photos.add(UserPhoto.at(url, photos.size()));
        touch();
    }

    public void removePhoto(long photoId) {
        boolean removed = photos.removeIf(photo -> photo.getId() != null && photo.getId() == photoId);
        if (!removed) {
            throw new IllegalArgumentException("Фотография не найдена в анкете");
        }
        for (int position = 0; position < photos.size(); position++) {
            photos.get(position).moveTo(position);
        }
        touch();
    }

    public boolean practises(long sportId) {
        return sports.stream().anyMatch(sport -> sport.getSportId() == sportId);
    }

    public int getAge() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public Long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getAbout() {
        return about;
    }

    public List<UserPhoto> getPhotos() {
        return Collections.unmodifiableList(photos);
    }

    public Set<UserSport> getSports() {
        return Collections.unmodifiableSet(sports);
    }

    public Set<UserGym> getGyms() {
        return Collections.unmodifiableSet(gyms);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private static String checkName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Имя обязательно");
        }
        return value.trim();
    }

    private static LocalDate checkBirthDate(LocalDate value) {
        if (value == null) {
            throw new IllegalArgumentException("Дата рождения обязательна");
        }
        if (!value.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Дата рождения должна быть в прошлом");
        }
        return value;
    }

    private static String normalizeAbout(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static List<UserPhoto> checkPhotos(Collection<UserPhoto> value) {
        List<UserPhoto> result = new ArrayList<>(value == null ? List.of() : value);
        if (result.size() > MAX_PHOTOS) {
            throw new IllegalArgumentException("Не больше " + MAX_PHOTOS + " фотографий в анкете");
        }
        long distinctPositions = result.stream().map(UserPhoto::getPosition).distinct().count();
        if (distinctPositions != result.size()) {
            throw new IllegalArgumentException("Позиции фотографий не должны повторяться");
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserProfile profile)) {
            return false;
        }
        return userId == profile.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "UserProfile{id=" + id + ", userId=" + userId + ", name=" + name + "}";
    }
}
