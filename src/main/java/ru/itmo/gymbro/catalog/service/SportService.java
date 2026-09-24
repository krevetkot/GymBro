package ru.itmo.gymbro.catalog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.catalog.api.SportUseCases;
import ru.itmo.gymbro.catalog.model.Sport;
import ru.itmo.gymbro.catalog.repository.SportRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.util.Set;

@Service
@Transactional(readOnly = true)
class SportService implements SportUseCases {

    private final SportRepository sports;

    SportService(SportRepository sports) {
        this.sports = sports;
    }

    @Override
    @Transactional
    public Sport create(String name) {
        Sport candidate = Sport.of(name);
        if (sports.existsByName(candidate.getName())) {
            throw new ConflictException("Вид спорта с таким названием уже существует");
        }
        return sports.save(candidate);
    }

    @Override
    public Sport getById(long id) {
        return sports.findById(id)
                .orElseThrow(() -> new NotFoundException("Вид спорта " + id + " не найден"));
    }

    @Override
    public Page<Sport> getPage(Pageable pageable) {
        return sports.findAll(CatalogPagination.validate(pageable, Set.of("id", "name")));
    }

    @Override
    @Transactional
    public void delete(long id) {
        getById(id);
        sports.deleteById(id);
    }
}

