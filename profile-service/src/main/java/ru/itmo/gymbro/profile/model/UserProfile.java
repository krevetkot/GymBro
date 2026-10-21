package ru.itmo.gymbro.profile.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "user_profiles")
@Getter
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Positive
    private long userId;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @Past
    private LocalDate birthDate;

    @Column(columnDefinition = "text")
    private String about;

    @Valid
    @NotNull
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "profile_id", nullable = false)
    private Set<UserSport> sports;

    @Valid
    @NotNull
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "profile_id", nullable = false)
    private Set<UserGym> gyms;

    @NotNull
    private Instant updatedAt;

    protected UserProfile() {
    }

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
        Collection<UserSport> wanted = newSports == null ? Set.of() : newSports;
        sports.removeIf(sport -> !wanted.contains(sport));
        for (UserSport sport : wanted) {
            sports.stream()
                    .filter(sport::equals)
                    .findFirst()
                    .ifPresentOrElse(existing -> existing.changeLevel(sport.getLevel()), () -> sports.add(sport));
        }
        touch();
    }

    public void replaceGyms(Collection<UserGym> newGyms) {
        Collection<UserGym> wanted = newGyms == null ? Set.of() : newGyms;
        gyms.removeIf(gym -> !wanted.contains(gym));
        gyms.addAll(wanted);
        touch();
    }

    public int getAge() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public Set<UserSport> getSports() {
        return Collections.unmodifiableSet(sports);
    }

    public Set<UserGym> getGyms() {
        return Collections.unmodifiableSet(gyms);
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
