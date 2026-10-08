package ru.itmo.gymbro.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.gymbro.identity.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT user FROM User user WHERE user.email = LOWER(TRIM(:email))")
    public Optional<User> findByEmail(String email);

    @Query("SELECT (COUNT(user) > 0) FROM User user WHERE user.email = LOWER(TRIM(:email))")
    public boolean existsByEmail(String email);

    @Query(value = "SELECT id FROM users WHERE status = 'BANNED'", nativeQuery = true)
    public List<Long> findBannedIds();
}
