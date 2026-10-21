package ru.itmo.gymbro.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.identity.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndDeletedFalse(long id);

    Page<User> findAllByDeletedFalse(Pageable pageable);

    @Query("SELECT user FROM User user WHERE user.email = LOWER(TRIM(:email))")
    public Optional<User> findByEmail(String email);

    @Query("SELECT (COUNT(user) > 0) FROM User user WHERE user.email = LOWER(TRIM(:email))")
    public boolean existsByEmail(String email);

    @Query(value = "SELECT id FROM users WHERE status = 'BANNED' OR deleted", nativeQuery = true)
    public List<Long> findBannedIds();
}
