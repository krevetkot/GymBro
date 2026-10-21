package ru.itmo.gymbro.catalog.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.catalog.api.CatalogDirectory;
import ru.itmo.gymbro.catalog.api.GymSnapshot;
import ru.itmo.gymbro.catalog.api.GymUseCases;
import ru.itmo.gymbro.catalog.api.SportSnapshot;
import ru.itmo.gymbro.catalog.api.SportUseCases;

@Service
class CatalogDirectoryService implements CatalogDirectory {

    private final SportUseCases sports;
    private final GymUseCases gyms;

    CatalogDirectoryService(SportUseCases sports, GymUseCases gyms) {
        this.sports = sports;
        this.gyms = gyms;
    }

    @Override
    public Mono<SportSnapshot> getSport(long id) {
        return sports.getById(id).map(sport -> new SportSnapshot(sport.getId(), sport.getName()));
    }

    @Override
    public Mono<GymSnapshot> getGym(long id) {
        return gyms.getById(id)
                .map(gym -> new GymSnapshot(gym.getId(), gym.getName(), gym.getCity(), gym.getAddress()));
    }
}
