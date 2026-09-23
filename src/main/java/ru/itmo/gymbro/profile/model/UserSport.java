package ru.itmo.gymbro.profile.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

@Table("user_sports")
public class UserSport {

    @Id
    private Long id;

    @Positive
    private long sportId;

    @NotNull
    private SportLevel level;

    public UserSport(Long id, long sportId, SportLevel level) {
        if (sportId <= 0) {
            throw new IllegalArgumentException("Вид спорта обязателен");
        }
        if (level == null) {
            throw new IllegalArgumentException("Уровень обязателен");
        }
        this.id = id;
        this.sportId = sportId;
        this.level = level;
    }

    public static UserSport of(long sportId, SportLevel level) {
        return new UserSport(null, sportId, level);
    }

    public void changeLevel(SportLevel newLevel) {
        if (newLevel == null) {
            throw new IllegalArgumentException("Уровень обязателен");
        }
        this.level = newLevel;
    }

    public Long getId() {
        return id;
    }

    public long getSportId() {
        return sportId;
    }

    public SportLevel getLevel() {
        return level;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserSport sport)) {
            return false;
        }
        return sportId == sport.sportId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sportId);
    }

    @Override
    public String toString() {
        return "UserSport{sportId=" + sportId + ", level=" + level + "}";
    }
}
