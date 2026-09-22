package ru.itmo.gymbro.identity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.identity.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(long id);

    Optional<User> findByEmail(String email);

    boolean existsById(long id);

    boolean existsByEmail(String email);

    Page<User> findAll(Pageable pageable);

    void deleteById(long id);
}
