package ru.itmo.gymbro.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.catalog.api.GymRequestUseCases;
import ru.itmo.gymbro.catalog.model.GymRequest;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.GymRequestRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.util.Set;

@Service
@RequiredArgsConstructor
class GymRequestService implements GymRequestUseCases {

    private final GymRequestRepository requests;
    private final GymRepository gyms;
    private final CurrentUserAccess access;
    private final TransactionalOperator transaction;

    @Override
    public Mono<GymRequest> submit(String name, String city, String address) {
        return access.requireActiveUser()
                .map(authorId -> GymRequest.submit(authorId, name, city, address))
                .flatMap(request -> requireAddressIsFree(request).then(Mono.defer(() -> requests.save(request))));
    }

    @Override
    public Mono<Page<GymRequest>> getPage(Pageable pageable) {
        return access.requireAdmin().then(Mono.defer(() -> CatalogPagination.page(pageable,
                Set.of("id", "authorId", "name", "city", "address", "status", "createdAt"),
                requests::findAllBy, requests.count())));
    }

    @Override
    public Mono<GymRequest> approve(long requestId) {
        return access.requireAdmin().flatMap(adminId -> pendingRequestForUpdate(requestId)
                .flatMap(request -> requireAddressIsFree(request)
                        .then(Mono.defer(() -> gyms.save(request.toGym())))
                        .flatMap(gym -> {
                            request.approve(adminId, gym.getId());
                            return requests.save(request);
                        }))
                .as(transaction::transactional));
    }

    @Override
    public Mono<GymRequest> reject(long requestId) {
        return access.requireAdmin().flatMap(adminId -> pendingRequestForUpdate(requestId)
                .flatMap(request -> {
                    request.reject(adminId);
                    return requests.save(request);
                })
                .as(transaction::transactional));
    }

    private Mono<GymRequest> pendingRequestForUpdate(long requestId) {
        return requests.findByIdForUpdate(requestId)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Заявка " + requestId + " не найдена")))
                .flatMap(request -> request.isPending()
                        ? Mono.just(request)
                        : Mono.error(new ConflictException("Заявка уже рассмотрена")));
    }

    private Mono<Void> requireAddressIsFree(GymRequest request) {
        return gyms.existsByCityAndAddress(request.getCity(), request.getAddress())
                .flatMap(exists -> exists
                        ? Mono.error(new ConflictException("Зал по этому адресу уже есть в каталоге"))
                        : Mono.empty());
    }
}
