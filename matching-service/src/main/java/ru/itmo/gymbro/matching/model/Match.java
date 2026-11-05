package ru.itmo.gymbro.matching.model;

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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

@Entity
@Table(name = "matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Positive
    @EqualsAndHashCode.Include
    @ToString.Include(name = "user1")
    private long user1Id;

    @Positive
    @EqualsAndHashCode.Include
    @ToString.Include(name = "user2")
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

    public long partnerOf(long userId) {
        if (user1Id == userId) {
            return user2Id;
        }
        if (user2Id == userId) {
            return user1Id;
        }
        throw new IllegalArgumentException("Пользователь не участвует в этом мэтче");
    }
}
