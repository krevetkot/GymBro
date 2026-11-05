package ru.itmo.gymbro.identity.dto;

import ru.itmo.gymbro.identity.model.Certificate;
import ru.itmo.gymbro.identity.model.CertificateStatus;

import java.time.Instant;

public record CertificateResponse(
        long id,
        long trainerId,
        long sportId,
        String fileUrl,
        CertificateStatus status,
        Long reviewedBy,
        Instant createdAt) {

    public static CertificateResponse from(Certificate certificate) {
        return new CertificateResponse(certificate.getId(), certificate.getTrainerId(), certificate.getSportId(),
                certificate.getFileUrl(), certificate.getStatus(), certificate.getReviewedBy(),
                certificate.getCreatedAt());
    }
}
