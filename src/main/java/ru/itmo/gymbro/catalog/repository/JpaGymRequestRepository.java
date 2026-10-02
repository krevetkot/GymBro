package ru.itmo.gymbro.catalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.itmo.gymbro.catalog.model.GymRequest;

import java.util.Optional;

@Repository
class JpaGymRequestRepository implements GymRequestRepository {

    private final GymRequestDao dao;

    JpaGymRequestRepository(GymRequestDao dao) {
        this.dao = dao;
    }

    @Override
    public GymRequest save(GymRequest request) {
        return dao.saveAndFlush(request);
    }

    @Override
    public Optional<GymRequest> findById(long id) {
        return dao.findById(id);
    }

    @Override
    public Optional<GymRequest> findByIdForUpdate(long id) {
        return dao.findLockedById(id);
    }

    @Override
    public Page<GymRequest> findAll(Pageable pageable) {
        return dao.findAll(pageable);
    }

    @Override
    public void deleteById(long id) {
        dao.deleteById(id);
        dao.flush();
    }
}
