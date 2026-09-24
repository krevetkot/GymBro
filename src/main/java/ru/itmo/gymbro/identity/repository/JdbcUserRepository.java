package ru.itmo.gymbro.identity.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.itmo.gymbro.identity.model.User;

import java.util.List;
import java.util.Optional;

@Repository
class JdbcUserRepository implements UserRepository {

    private final UserDao dao;

    JdbcUserRepository(UserDao dao) {
        this.dao = dao;
    }

    @Override
    public User save(User user) {
        return dao.save(user);
    }

    @Override
    public Optional<User> findById(long id) {
        return dao.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return dao.findByEmail(email == null ? null : email.trim().toLowerCase());
    }

    @Override
    public boolean existsById(long id) {
        return dao.existsById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return dao.existsByEmail(email == null ? null : email.trim().toLowerCase());
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return dao.findAll(pageable);
    }

    @Override
    public void deleteById(long id) {
        dao.deleteById(id);
    }

    @Override
    public List<Long> findBannedIds() {
        return dao.findBannedIds();
    }
}
