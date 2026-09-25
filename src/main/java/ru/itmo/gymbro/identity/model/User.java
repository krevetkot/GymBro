package ru.itmo.gymbro.identity.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;

@Table("users")
public class User {

    @Id
    private Long id;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 255)
    private String password;

    @NotNull
    private Role role;

    @NotNull
    private UserStatus status;

    @NotNull
    private Instant createdAt;

    public User(Long id, String email, String password, Role role, UserStatus status, Instant createdAt) {
        this.id = id;
        this.email = normalizeEmail(email);
        this.password = checkPassword(password);
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

    public static User register(String email, String password) {
        return new User(null, email, password, Role.USER, UserStatus.ACTIVE, Instant.now());
    }

    public void changeEmail(String newEmail) {
        this.email = normalizeEmail(newEmail);
    }

    public void changePassword(String newPassword) {
        this.password = checkPassword(newPassword);
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

    public String getPassword() {
        return password;
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
