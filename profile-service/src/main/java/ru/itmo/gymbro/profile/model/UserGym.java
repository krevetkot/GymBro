package ru.itmo.gymbro.profile.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "user_gyms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class UserGym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Positive
    @EqualsAndHashCode.Include
    @ToString.Include
    private long gymId;

    public UserGym(Long id, long gymId) {
        if (gymId <= 0) {
            throw new IllegalArgumentException("Зал обязателен");
        }
        this.id = id;
        this.gymId = gymId;
    }

    public static UserGym of(long gymId) {
        return new UserGym(null, gymId);
    }
}
