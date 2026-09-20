package ru.itmo.gymbro.profile.model;

import java.util.Objects;

public class UserGym {

    private Long id;
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
