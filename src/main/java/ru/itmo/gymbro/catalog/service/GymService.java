package ru.itmo.gymbro.catalog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.catalog.api.GymUseCases;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.util.Set;

@Service
@Transactional(readOnly = true)
class GymService implements GymUseCases {

    private final GymRepository gyms;
    private final CurrentUserAccess access;

    GymService(GymRepository gyms, CurrentUserAccess access) {
        this.gyms = gyms;
        this.access = access;
    }

    @Override
    @Transactional
    public Gym create(String name, String city, String address) {
        Gym candidate = Gym.of(name, city, address);
        if (gyms.existsByCityAndAddress(candidate.getCity(), candidate.getAddress())) {
            throw new ConflictException("Зал по этому адресу уже существует");
        }
        return gyms.save(candidate);
    }

    @Override
    public Gym getById(long id) {
        return gyms.findById(id)
                .orElseThrow(() -> new NotFoundException("Зал " + id + " не найден"));
    }

    @Override
    public Page<Gym> getPage(Pageable pageable) {
        return gyms.findAll(CatalogPagination.validate(pageable, Set.of("id", "name", "city", "address")));
    }

    @Override
    @Transactional
    public Gym update(long id, String name, String city, String address) {
        access.requireAdmin();
        Gym existing = getById(id);
        Gym candidate = new Gym(id, name, city, address);
        if ((!existing.getCity().equals(candidate.getCity())
                || !existing.getAddress().equals(candidate.getAddress()))
                && gyms.existsByCityAndAddress(candidate.getCity(), candidate.getAddress())) {
            throw new ConflictException("Зал по этому адресу уже существует");
        }
        existing.rename(candidate.getName());
        existing.relocate(candidate.getCity(), candidate.getAddress());
        return gyms.save(existing);
    }

    @Override
    @Transactional
    public void delete(long id) {
        access.requireAdmin();
        getById(id);
        gyms.deleteById(id);
    }
}
