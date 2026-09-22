package ru.itmo.gymbro.matching.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;

@Table("likes")
public class Like {

    @Id
    private Long id;
    private long fromUserId;
    private long toUserId;
    private Instant createdAt;

    public Like(Long id, long fromUserId, long toUserId, Instant createdAt) {
        if (fromUserId <= 0 || toUserId <= 0) {
            throw new IllegalArgumentException("Лайк должен связывать двух пользователей");
        }
        if (fromUserId == toUserId) {
            throw new IllegalArgumentException("Нельзя лайкнуть самого себя");
        }
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static Like from(long fromUserId, long toUserId) {
        return new Like(null, fromUserId, toUserId, Instant.now());
    }

    public boolean isReciprocalTo(Like other) {
        return other != null && fromUserId == other.toUserId && toUserId == other.fromUserId;
    }

    public Long getId() {
        return id;
    }

    public long getFromUserId() {
        return fromUserId;
    }

    public long getToUserId() {
        return toUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Like like)) {
            return false;
        }
        return fromUserId == like.fromUserId && toUserId == like.toUserId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromUserId, toUserId);
    }

    @Override
    public String toString() {
        return "Like{from=" + fromUserId + ", to=" + toUserId + "}";
    }
}
