package ru.itmo.gymbro.identity.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @NotBlank
    @Email
    @Size(max = 255)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String email;

    @NotBlank
    @Size(max = 255)
    @JsonIgnore
    private String password;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    @ToString.Include
    private Role role;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    @ToString.Include
    private UserStatus status;

    @NotNull
    private Instant createdAt;

    @Column(nullable = false)
    private boolean deleted;

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
        return register(email, password, Role.USER);
    }

    public static User register(String email, String password, Role role) {
        return new User(null, email, password, role, UserStatus.ACTIVE, Instant.now());
    }

    public void changeEmail(String newEmail) {
        this.email = normalizeEmail(newEmail);
    }

    public void changePassword(String newPassword) {
        this.password = checkPassword(newPassword);
    }

    public void changeRole(Role newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("Роль обязательна");
        }
        this.role = newRole;
    }

    public void ban() {
        this.status = UserStatus.BANNED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public boolean isActive() {
        return !deleted && status == UserStatus.ACTIVE;
    }

    public void delete() {
        deleted = true;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
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
}
