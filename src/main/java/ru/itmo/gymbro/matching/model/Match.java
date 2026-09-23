package ru.itmo.gymbro.matching.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;

@Table("matches")
public class Match {

    @Id
    private Long id;

    @Positive
    private long user1Id;

    @Positive
    private long user2Id;

    @NotNull
    private Instant createdAt;

    public Match(Long id, long user1Id, long user2Id, Instant createdAt) {
        if (user1Id <= 0 || user2Id <= 0) {
            throw new IllegalArgumentException("Мэтч должен связывать двух пользователей");
        }
        if (user1Id >= user2Id) {
            throw new IllegalArgumentException("Идентификаторы пары должны быть упорядочены по возрастанию");
        }
        this.id = id;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static Match between(long oneUserId, long anotherUserId) {
        if (oneUserId == anotherUserId) {
            throw new IllegalArgumentException("Нельзя создать мэтч с самим собой");
        }
        return new Match(null,
                Math.min(oneUserId, anotherUserId),
                Math.max(oneUserId, anotherUserId),
                Instant.now());
    }

    public boolean involves(long userId) {
        return user1Id == userId || user2Id == userId;
    }

    public long partnerOf(long userId) {
        if (user1Id == userId) {
            return user2Id;
        }
        if (user2Id == userId) {
            return user1Id;
        }
        throw new IllegalArgumentException("Пользователь не участвует в этом мэтче");
    }

    public Long getId() {
        return id;
    }

    public long getUser1Id() {
        return user1Id;
    }

    public long getUser2Id() {
        return user2Id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Match match)) {
            return false;
        }
        return user1Id == match.user1Id && user2Id == match.user2Id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(user1Id, user2Id);
    }

    @Override
    public String toString() {
        return "Match{id=" + id + ", user1=" + user1Id + ", user2=" + user2Id + "}";
    }
}
