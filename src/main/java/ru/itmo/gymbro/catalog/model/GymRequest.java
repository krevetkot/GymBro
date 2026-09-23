package ru.itmo.gymbro.catalog.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;

@Table("gym_requests")
public class GymRequest {

    @Id
    private Long id;

    @Positive
    private long authorId;

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(max = 300)
    private String address;

    @NotNull
    private RequestStatus status;

    @Positive
    private Long reviewedBy;

    @Positive
    private Long gymId;

    @NotNull
    private Instant createdAt;

    public GymRequest(Long id, long authorId, String name, String city, String address,
                      RequestStatus status, Long reviewedBy, Long gymId, Instant createdAt) {
        if (authorId <= 0) {
            throw new IllegalArgumentException("Заявка должна иметь автора");
        }
        if (status == null) {
            throw new IllegalArgumentException("Статус заявки обязателен");
        }
        if (status != RequestStatus.PENDING && reviewedBy == null) {
            throw new IllegalArgumentException("Рассмотренная заявка должна содержать администратора");
        }
        if (status == RequestStatus.APPROVED && gymId == null) {
            throw new IllegalArgumentException("Одобренная заявка должна ссылаться на созданный зал");
        }
        if (status != RequestStatus.APPROVED && gymId != null) {
            throw new IllegalArgumentException("Ссылка на зал допустима только у одобренной заявки");
        }
        this.id = id;
        this.authorId = authorId;
        this.name = checkText(name, "Название зала обязательно");
        this.city = checkText(city, "Город обязателен");
        this.address = checkText(address, "Адрес обязателен");
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.gymId = gymId;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static GymRequest submit(long authorId, String name, String city, String address) {
        return new GymRequest(null, authorId, name, city, address,
                RequestStatus.PENDING, null, null, Instant.now());
    }

    public void approve(long adminId, long createdGymId) {
        requirePending();
        this.status = RequestStatus.APPROVED;
        this.reviewedBy = adminId;
        this.gymId = createdGymId;
    }

    public void reject(long adminId) {
        requirePending();
        this.status = RequestStatus.REJECTED;
        this.reviewedBy = adminId;
    }

    public Gym toGym() {
        return Gym.of(name, city, address);
    }

    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public long getAuthorId() {
        return authorId;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public Long getGymId() {
        return gymId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private void requirePending() {
        if (status != RequestStatus.PENDING) {
            throw new IllegalStateException("Заявка уже рассмотрена");
        }
    }

    private static String checkText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GymRequest request)) {
            return false;
        }
        return id != null && id.equals(request.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "GymRequest{id=" + id + ", author=" + authorId + ", status=" + status + "}";
    }
}
