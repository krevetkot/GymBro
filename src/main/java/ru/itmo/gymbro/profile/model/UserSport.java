package ru.itmo.gymbro.profile.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Objects;

@Entity
@Table(name = "user_sports")
public class UserSport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Positive
    private long sportId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private SportLevel level;

    protected UserSport() {
    }

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

    void changeLevel(SportLevel newLevel) {
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
