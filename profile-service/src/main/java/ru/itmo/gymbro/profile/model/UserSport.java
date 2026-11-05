package ru.itmo.gymbro.profile.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "user_sports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class UserSport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Positive
    @EqualsAndHashCode.Include
    @ToString.Include
    private long sportId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    @ToString.Include
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

    void changeLevel(SportLevel newLevel) {
        this.level = newLevel;
    }
}
