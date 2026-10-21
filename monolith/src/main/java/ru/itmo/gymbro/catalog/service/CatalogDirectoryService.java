package ru.itmo.gymbro.catalog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.catalog.api.CatalogDirectory;
import ru.itmo.gymbro.catalog.api.GymSnapshot;
import ru.itmo.gymbro.catalog.api.GymUseCases;
import ru.itmo.gymbro.catalog.api.SportSnapshot;
import ru.itmo.gymbro.catalog.api.SportUseCases;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.model.Sport;

@Service
@Transactional(readOnly = true)
class CatalogDirectoryService implements CatalogDirectory {

    private final SportUseCases sports;
    private final GymUseCases gyms;

    CatalogDirectoryService(SportUseCases sports, GymUseCases gyms) {
        this.sports = sports;
        this.gyms = gyms;
    }

    @Override
    public SportSnapshot getSport(long id) {
        Sport sport = sports.getById(id);
        return new SportSnapshot(sport.getId(), sport.getName());
    }

    @Override
    public GymSnapshot getGym(long id) {
        Gym gym = gyms.getById(id);
        return new GymSnapshot(gym.getId(), gym.getName(), gym.getCity(), gym.getAddress());
    }
}
