package ru.itmo.gymbro.profile.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;

import java.util.Objects;

@Entity
@Table(name = "user_gyms")
public class UserGym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Positive
    private long gymId;

    protected UserGym() {
    }

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

    public Long getId() {
        return id;
    }

    public long getGymId() {
        return gymId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserGym gym)) {
            return false;
        }
        return gymId == gym.gymId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gymId);
    }

    @Override
    public String toString() {
        return "UserGym{gymId=" + gymId + "}";
    }
}
