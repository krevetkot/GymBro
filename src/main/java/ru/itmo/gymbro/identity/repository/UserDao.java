package ru.itmo.gymbro.identity.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import ru.itmo.gymbro.identity.model.User;

import java.util.List;
import java.util.Optional;

interface UserDao extends ListCrudRepository<User, Long>, PagingAndSortingRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT id FROM users WHERE status = 'BANNED'")
    List<Long> findBannedIds();
}
