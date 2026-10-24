package ru.itmo.gymbro.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.catalog.api.SportUseCases;
import ru.itmo.gymbro.catalog.model.Sport;
import ru.itmo.gymbro.catalog.repository.SportRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.util.Set;

@Service
@RequiredArgsConstructor
class SportService implements SportUseCases {

    private final SportRepository sports;

    @Override
    public Mono<Sport> create(String name) {
        return Mono.fromCallable(() -> Sport.of(name))
                .flatMap(candidate -> sports.existsByName(candidate.getName())
                        .flatMap(exists -> exists
                                ? Mono.error(new ConflictException("Вид спорта с таким названием уже существует"))
                                : sports.save(candidate)));
    }

    @Override
    public Mono<Sport> getById(long id) {
        return sports.findById(id)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Вид спорта " + id + " не найден")));
    }

    @Override
    public Mono<Page<Sport>> getPage(Pageable pageable) {
        return CatalogPagination.page(pageable, Set.of("id", "name"), sports::findAllBy, sports.count());
    }

    @Override
    public Mono<Void> delete(long id) {
        return getById(id).flatMap(sports::delete);
    }
}
