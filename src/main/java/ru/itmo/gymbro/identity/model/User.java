package ru.itmo.gymbro.identity.model;

import java.time.Instant;
import java.util.Objects;

public class User {

    private Long id;
    private String email;
    private String passwordHash;
    private Role role;
    private UserStatus status;
    private Instant createdAt;

    public User(Long id, String email, String passwordHash, Role role, UserStatus status, Instant createdAt) {
        this.id = id;
        this.email = normalizeEmail(email);
        this.passwordHash = checkPassword(passwordHash);
        if (role == null) {
            throw new IllegalArgumentException("Роль обязательна");
        }
        if (status == null) {
            throw new IllegalArgumentException("Статус обязателен");
        }
        this.role = role;
        this.status = status;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static User register(String email, String passwordHash) {
        return new User(null, email, passwordHash, Role.USER, UserStatus.ACTIVE, Instant.now());
    }

    public void changeEmail(String newEmail) {
        this.email = normalizeEmail(newEmail);
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = checkPassword(newPasswordHash);
    }

    public void promoteToTrainer() {
        this.role = Role.TRAINER;
    }

    public void ban() {
        this.status = UserStatus.BANNED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email обязателен");
        }
        return value.trim().toLowerCase();
    }

    private static String checkPassword(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Пароль обязателен");
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return email.equals(user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", email=" + email + ", role=" + role + ", status=" + status + "}";
    }
}
