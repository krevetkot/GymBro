package ru.itmo.gymbro.identity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Сертификат или диплом, который пользователь прикладывает, чтобы стать тренером.
 * Администратор одобряет его (пользователь получает роль TRAINER) или отклоняет.
 * {@code sportId} ссылается на вид спорта из catalog-service — это другая база, поэтому без внешнего ключа.
 */
@Entity
@Table(name = "certificates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Positive
    private long trainerId;

    @Positive
    private long sportId;

    @NotBlank
    @Size(max = 500)
    private String fileUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private CertificateStatus status;

    private Long reviewedBy;

    @NotNull
    private Instant createdAt;

    private Certificate(long trainerId, long sportId, String fileUrl) {
        if (trainerId <= 0 || sportId <= 0) {
            throw new IllegalArgumentException("Идентификаторы пользователя и вида спорта должны быть положительными");
        }
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("Ссылка на файл сертификата обязательна");
        }
        this.trainerId = trainerId;
        this.sportId = sportId;
        this.fileUrl = fileUrl.trim();
        this.status = CertificateStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public static Certificate submit(long trainerId, long sportId, String fileUrl) {
        return new Certificate(trainerId, sportId, fileUrl);
    }

    public boolean isPending() {
        return status == CertificateStatus.PENDING;
    }

    public void approve(long adminId) {
        review(adminId, CertificateStatus.APPROVED);
    }

    public void reject(long adminId) {
        review(adminId, CertificateStatus.REJECTED);
    }

    private void review(long adminId, CertificateStatus decision) {
        if (!isPending()) {
            throw new IllegalStateException("Сертификат уже рассмотрен");
        }
        this.status = decision;
        this.reviewedBy = adminId;
    }
}
