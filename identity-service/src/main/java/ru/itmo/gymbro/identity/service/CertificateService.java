package ru.itmo.gymbro.identity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.identity.api.CertificateUseCases;
import ru.itmo.gymbro.identity.model.Certificate;
import ru.itmo.gymbro.identity.model.CertificateStatus;
import ru.itmo.gymbro.identity.model.Role;
import ru.itmo.gymbro.identity.model.User;
import ru.itmo.gymbro.identity.repository.CertificateRepository;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;
import ru.itmo.gymbro.shared.persistence.BlockingJpa;

@Service
@RequiredArgsConstructor
class CertificateService implements CertificateUseCases {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "id");

    private final CertificateRepository certificates;
    private final UserRepository users;
    private final CurrentUserAccess access;
    private final BlockingJpa jpa;

    @Override
    public Mono<Certificate> submit(long sportId, String fileUrl) {
        return access.requireActiveUser().flatMap(userId -> jpa.write(() -> {
            User author = users.findByIdAndDeletedFalse(userId)
                    .orElseThrow(() -> new NotFoundException("Пользователь " + userId + " не найден"));
            if (author.isAdmin()) {
                throw new ForbiddenException("Администратору сертификат тренера не нужен");
            }
            if (certificates.existsByTrainerIdAndSportIdAndStatus(userId, sportId, CertificateStatus.PENDING)) {
                throw new ConflictException("Сертификат по этому виду спорта уже ждёт проверки");
            }
            return certificates.save(Certificate.submit(userId, sportId, fileUrl));
        }));
    }

    @Override
    public Mono<Page<Certificate>> getMine(Pageable pageable) {
        return access.requireActiveUser()
                .flatMap(userId -> jpa.read(() -> certificates.findAllByTrainerId(userId, newestFirst(pageable))));
    }

    @Override
    public Mono<Page<Certificate>> getPage(CertificateStatus status, Pageable pageable) {
        return access.requireAdmin().then(jpa.read(() -> status == null
                ? certificates.findAll(newestFirst(pageable))
                : certificates.findAllByStatus(status, newestFirst(pageable))));
    }

    /**
     * Транзакция: сертификат блокируется ({@code SELECT ... FOR UPDATE}), получает статус APPROVED,
     * автор получает роль TRAINER — всё или ничего. Без неё роль могла бы смениться без одобренного
     * сертификата, а два администратора — одобрить один сертификат дважды.
     */
    @Override
    public Mono<Certificate> approve(long certificateId) {
        return access.requireAdmin().flatMap(adminId -> jpa.write(() -> {
            Certificate certificate = pendingForUpdate(certificateId);
            User author = users.findByIdAndDeletedFalse(certificate.getTrainerId())
                    .filter(User::isActive)
                    .orElseThrow(() -> new ConflictException("Автор сертификата заблокирован или удалён"));
            certificate.approve(adminId);
            if (author.getRole() == Role.USER) {
                author.changeRole(Role.TRAINER);
                users.save(author);
            }
            return certificates.save(certificate);
        }));
    }

    @Override
    public Mono<Certificate> reject(long certificateId) {
        return access.requireAdmin().flatMap(adminId -> jpa.write(() -> {
            Certificate certificate = pendingForUpdate(certificateId);
            certificate.reject(adminId);
            return certificates.save(certificate);
        }));
    }

    private Certificate pendingForUpdate(long certificateId) {
        Certificate certificate = certificates.findByIdForUpdate(certificateId)
                .orElseThrow(() -> new NotFoundException("Сертификат " + certificateId + " не найден"));
        if (!certificate.isPending()) {
            throw new ConflictException("Сертификат уже рассмотрен");
        }
        return certificate;
    }

    private static Pageable newestFirst(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
    }
}
