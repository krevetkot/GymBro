package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.itmo.gymbro.catalog.model.Gym;

import java.util.Optional;

@Repository
class JdbcGymRepository implements GymRepository {

    private final GymDao dao;

    JdbcGymRepository(GymDao dao) {
        this.dao = dao;
    }

    @Override
    public Gym save(Gym gym) {
        return dao.save(gym);
    }

    @Override
    public Optional<Gym> findById(long id) {
        return dao.findById(id);
    }

    @Override
    public Page<Gym> findAll(Pageable pageable) {
        return dao.findAll(pageable);
    }

    @Override
    public boolean existsById(long id) {
        return dao.existsById(id);
    }

    @Override
    public boolean existsByCityAndAddress(String city, String address) {
        return dao.existsByCityAndAddress(
                city == null ? null : city.trim(),
                address == null ? null : address.trim());
    }

    @Override
    public void deleteById(long id) {
        dao.deleteById(id);
    }
}
