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
@Table(name = "likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Positive
    @EqualsAndHashCode.Include
    @ToString.Include(name = "from")
    private long fromUserId;

    @Positive
    @EqualsAndHashCode.Include
    @ToString.Include(name = "to")
    private long toUserId;

    @NotNull
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
}
