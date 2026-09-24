package ru.itmo.gymbro.catalog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.catalog.api.GymRequestUseCases;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.model.GymRequest;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.GymRequestRepository;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.util.Set;

@Service
@Transactional(readOnly = true)
class GymRequestService implements GymRequestUseCases {

    private final GymRequestRepository requests;
    private final GymRepository gyms;
    private final CurrentUserAccess access;

    GymRequestService(GymRequestRepository requests, GymRepository gyms, CurrentUserAccess access) {
        this.requests = requests;
        this.gyms = gyms;
        this.access = access;
    }

    @Override
    @Transactional
    public GymRequest submit(String name, String city, String address) {
        long authorId = access.requireActiveUser();
        GymRequest request = GymRequest.submit(authorId, name, city, address);
        requireAddressIsFree(request);
        return requests.save(request);
    }

    @Override
    public Page<GymRequest> getPage(Pageable pageable) {
        access.requireAdmin();
        return requests.findAll(CatalogPagination.validate(pageable,
                Set.of("id", "authorId", "name", "city", "address", "status", "createdAt")));
    }

    @Override
    @Transactional
    public GymRequest approve(long requestId) {
        long adminId = access.requireAdmin();
        GymRequest request = pendingRequestForUpdate(requestId);
        requireAddressIsFree(request);
        Gym gym = gyms.save(request.toGym());
        request.approve(adminId, gym.getId());
        return requests.save(request);
    }

    @Override
    @Transactional
    public GymRequest reject(long requestId) {
        long adminId = access.requireAdmin();
        GymRequest request = pendingRequestForUpdate(requestId);
        request.reject(adminId);
        return requests.save(request);
    }

    private GymRequest pendingRequestForUpdate(long requestId) {
        GymRequest request = requests.findByIdForUpdate(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка " + requestId + " не найдена"));
        if (!request.isPending()) {
            throw new ConflictException("Заявка уже рассмотрена");
        }
        return request;
    }

    private void requireAddressIsFree(GymRequest request) {
        if (gyms.existsByCityAndAddress(request.getCity(), request.getAddress())) {
            throw new ConflictException("Зал по этому адресу уже есть в каталоге");
        }
    }
}
