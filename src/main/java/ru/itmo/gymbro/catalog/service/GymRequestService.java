package ru.itmo.gymbro.catalog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.catalog.api.GymRequestUseCases;
import ru.itmo.gymbro.catalog.model.GymRequest;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.GymRequestRepository;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.ConflictException;

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
        if (gyms.existsByCityAndAddress(request.getCity(), request.getAddress())) {
            throw new ConflictException("Зал по этому адресу уже есть в каталоге");
        }
        return requests.save(request);
    }

    @Override
    public Page<GymRequest> getPage(Pageable pageable) {
        access.requireAdmin();
        return requests.findAll(CatalogPagination.validate(pageable,
                Set.of("id", "authorId", "name", "city", "address", "status", "createdAt")));
    }
}

