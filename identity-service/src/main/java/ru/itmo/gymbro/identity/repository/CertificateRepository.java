package ru.itmo.gymbro.identity.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.gymbro.identity.model.Certificate;
import ru.itmo.gymbro.identity.model.CertificateStatus;

import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    public Page<Certificate> findAllByTrainerId(long trainerId, Pageable pageable);

    public Page<Certificate> findAllByStatus(CertificateStatus status, Pageable pageable);

    public boolean existsByTrainerIdAndSportIdAndStatus(long trainerId, long sportId, CertificateStatus status);

    /** Блокирует строку до конца транзакции: два администратора не рассмотрят один сертификат одновременно. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT certificate FROM Certificate certificate WHERE certificate.id = :id")
    public Optional<Certificate> findByIdForUpdate(long id);
}
