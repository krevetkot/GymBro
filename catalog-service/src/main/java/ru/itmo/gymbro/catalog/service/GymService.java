package ru.itmo.gymbro.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.catalog.api.GymUseCases;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.util.Set;

@Service
@RequiredArgsConstructor
class GymService implements GymUseCases {

    private final GymRepository gyms;
    private final CurrentUserAccess access;

    @Override
    public Mono<Gym> create(String name, String city, String address) {
        return Mono.fromCallable(() -> Gym.of(name, city, address))
                .flatMap(candidate -> requireAddressIsFree(candidate).then(Mono.defer(() -> gyms.save(candidate))));
    }

    @Override
    public Mono<Gym> getById(long id) {
        return gyms.findById(id)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Зал " + id + " не найден")));
    }

    @Override
    public Mono<Page<Gym>> getPage(Pageable pageable) {
        return CatalogPagination.page(pageable, Set.of("id", "name", "city", "address"),
                gyms::findAllBy, gyms.count());
    }

    @Override
    public Mono<Gym> update(long id, String name, String city, String address) {
        return access.requireAdmin()
                .then(Mono.fromCallable(() -> new Gym(id, name, city, address)))
                .flatMap(candidate -> getById(id).flatMap(existing -> {
                    boolean moved = !existing.getCity().equals(candidate.getCity())
                            || !existing.getAddress().equals(candidate.getAddress());
                    Mono<Void> addressCheck = moved ? requireAddressIsFree(candidate) : Mono.empty();
                    return addressCheck.then(Mono.defer(() -> {
                        existing.rename(candidate.getName());
                        existing.relocate(candidate.getCity(), candidate.getAddress());
                        return gyms.save(existing);
                    }));
                }));
    }

    @Override
    public Mono<Void> delete(long id) {
        return access.requireAdmin()
                .then(Mono.defer(() -> getById(id)))
                .flatMap(gyms::delete);
    }

    private Mono<Void> requireAddressIsFree(Gym candidate) {
        return gyms.existsByCityAndAddress(candidate.getCity(), candidate.getAddress())
                .flatMap(exists -> exists
                        ? Mono.error(new ConflictException("Зал по этому адресу уже существует"))
                        : Mono.empty());
    }
}
