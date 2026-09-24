package ru.itmo.gymbro.catalog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.gymbro.catalog.api.ReviewGymRequestUseCase;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.model.GymRequest;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.GymRequestRepository;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.NotFoundException;

@Service
@Transactional
class ReviewGymRequestService implements ReviewGymRequestUseCase {

    private final GymRequestRepository requests;
    private final GymRepository gyms;
    private final CurrentUserAccess access;

    ReviewGymRequestService(GymRequestRepository requests, GymRepository gyms,
                            CurrentUserAccess access) {
        this.requests = requests;
        this.gyms = gyms;
        this.access = access;
    }

    @Override
    public GymRequest approve(long requestId) {
        long adminId = access.requireAdmin();
        GymRequest request = pendingRequestForUpdate(requestId);
        if (gyms.existsByCityAndAddress(request.getCity(), request.getAddress())) {
            throw new ConflictException("Зал по этому адресу уже есть в каталоге");
        }

        Gym gym = gyms.save(request.toGym());
        request.approve(adminId, gym.getId());
        return requests.save(request);
    }

    @Override
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
}
