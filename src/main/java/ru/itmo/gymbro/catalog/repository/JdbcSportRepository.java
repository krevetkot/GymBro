package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.itmo.gymbro.catalog.model.Sport;

import java.util.Optional;

@Repository
class JdbcSportRepository implements SportRepository {

    private final SportDao dao;

    JdbcSportRepository(SportDao dao) {
        this.dao = dao;
    }

    @Override
    public Sport save(Sport sport) {
        return dao.save(sport);
    }

    @Override
    public Optional<Sport> findById(long id) {
        return dao.findById(id);
    }

    @Override
    public Page<Sport> findAll(Pageable pageable) {
        return dao.findAll(pageable);
    }

    @Override
    public boolean existsById(long id) {
        return dao.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return dao.existsByName(name == null ? null : name.trim());
    }

    @Override
    public void deleteById(long id) {
        dao.deleteById(id);
    }
}
