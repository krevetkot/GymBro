package ru.itmo.gymbro.profile.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Table("user_profiles")
public class UserProfile {

    @Id
    private Long id;

    @Positive
    private long userId;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @Past
    private LocalDate birthDate;

    private String about;

    @Valid
    @NotNull
    @MappedCollection(idColumn = "profile_id")
    private Set<UserSport> sports;

    @Valid
    @NotNull
    @MappedCollection(idColumn = "profile_id")
    private Set<UserGym> gyms;

    @NotNull
    private Instant updatedAt;

    public UserProfile(Long id, long userId, String name, LocalDate birthDate, String about,
                       Set<UserSport> sports, Set<UserGym> gyms, Instant updatedAt) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Профиль должен быть привязан к пользователю");
        }
        this.id = id;
        this.userId = userId;
        this.name = checkName(name);
        this.birthDate = checkBirthDate(birthDate);
        this.about = normalizeAbout(about);
        this.sports = new LinkedHashSet<>(sports == null ? Set.of() : sports);
        this.gyms = new LinkedHashSet<>(gyms == null ? Set.of() : gyms);
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public static UserProfile create(long userId, String name, LocalDate birthDate, String about) {
        return new UserProfile(null, userId, name, birthDate, about, Set.of(), Set.of(), Instant.now());
    }

    public void edit(String newName, LocalDate newBirthDate, String newAbout) {
        this.name = checkName(newName);
        this.birthDate = checkBirthDate(newBirthDate);
        this.about = normalizeAbout(newAbout);
        touch();
    }

    public void replaceSports(Collection<UserSport> newSports) {
        this.sports = new LinkedHashSet<>(newSports == null ? Set.of() : newSports);
        touch();
    }

    public void replaceGyms(Collection<UserGym> newGyms) {
        this.gyms = new LinkedHashSet<>(newGyms == null ? Set.of() : newGyms);
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
