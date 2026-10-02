package ru.itmo.gymbro.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.gymbro.identity.model.User;

import java.util.List;
import java.util.Optional;

interface UserDao extends JpaRepository<User, Long> {

    public Optional<User> findByEmail(String email);

    public boolean existsByEmail(String email);

    @Query(value = "SELECT id FROM users WHERE status = 'BANNED'", nativeQuery = true)
    public List<Long> findBannedIds();
}
