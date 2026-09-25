package ru.itmo.gymbro.identity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.itmo.gymbro.identity.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    public User save(User user);

    public Optional<User> findById(long id);

    public Optional<User> findByEmail(String email);

    public boolean existsById(long id);

    public boolean existsByEmail(String email);

    public Page<User> findAll(Pageable pageable);

    public void deleteById(long id);

    public List<Long> findBannedIds();
}
