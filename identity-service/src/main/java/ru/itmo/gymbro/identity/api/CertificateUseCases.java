package ru.itmo.gymbro.identity.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.identity.model.Certificate;
import ru.itmo.gymbro.identity.model.CertificateStatus;

/** Сертификаты тренеров: пользователь прикладывает, администратор верифицирует. */
public interface CertificateUseCases {

    /** Приложить сертификат. Доступно USER и TRAINER; администратору сертификат не нужен. */
    public Mono<Certificate> submit(long sportId, String fileUrl);

    /** Свои сертификаты текущего пользователя. */
    public Mono<Page<Certificate>> getMine(Pageable pageable);

    /** Все сертификаты или только с указанным статусом. Только ADMIN. */
    public Mono<Page<Certificate>> getPage(CertificateStatus status, Pageable pageable);

    /** Одобрить: в одной транзакции сертификат становится APPROVED, а автор получает роль TRAINER. */
    public Mono<Certificate> approve(long certificateId);

    /** Отклонить сертификат; роль автора не меняется. */
    public Mono<Certificate> reject(long certificateId);
}
