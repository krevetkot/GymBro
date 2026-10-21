package ru.itmo.gymbro.catalog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.model.GymRequest;
import ru.itmo.gymbro.catalog.model.RequestStatus;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.GymRequestRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GymRequestServiceTest {

    private final GymRequestRepository requests = mock(GymRequestRepository.class);
    private final GymRepository gyms = mock(GymRepository.class);
    private final CurrentUserAccess access = mock(CurrentUserAccess.class);
    private final TransactionalOperator transaction = mock(TransactionalOperator.class);
    private final GymRequestService service = new GymRequestService(requests, gyms, access, transaction);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void passThroughTransaction() {
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsPendingRequestWithCurrentAuthorWithoutCreatingGym() {
        when(access.requireActiveUser()).thenReturn(Mono.just(7L));
        when(gyms.existsByCityAndAddress("City", "Address")).thenReturn(Mono.just(false));
        when(requests.save(any(GymRequest.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.submit(" Gym ", " City ", " Address "))
                .assertNext(request -> {
                    assertThat(request.getAuthorId()).isEqualTo(7L);
                    assertThat(request.getName()).isEqualTo("Gym");
                    assertThat(request.getCity()).isEqualTo("City");
                    assertThat(request.getAddress()).isEqualTo("Address");
                    assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
                    assertThat(request.getGymId()).isNull();
                    assertThat(request.getReviewedBy()).isNull();
                })
                .verifyComplete();
        verify(gyms, never()).save(any());
    }

    @Test
    void doesNotSaveRequestForExistingGym() {
        when(access.requireActiveUser()).thenReturn(Mono.just(7L));
        when(gyms.existsByCityAndAddress("City", "Address")).thenReturn(Mono.just(true));

        StepVerifier.create(service.submit("Gym", "City", "Address")).verifyError(ConflictException.class);
        verifyNoInteractions(requests);
    }

    @Test
    void checksAccessBeforeWritingOrReadingRequests() {
        when(access.requireActiveUser()).thenReturn(Mono.error(new ForbiddenException("Пользователь заблокирован")));
        when(access.requireAdmin()).thenReturn(Mono.error(new ForbiddenException("Нет прав")));

        StepVerifier.create(service.submit("Gym", "City", "Address")).verifyError(ForbiddenException.class);
        StepVerifier.create(service.getPage(PageRequest.of(0, 20))).verifyError(ForbiddenException.class);
        verifyNoInteractions(requests, gyms);
    }

    @Test
    void checksAdminBeforeLookingUpRequest() {
        when(access.requireAdmin()).thenReturn(Mono.error(new ForbiddenException("Нет прав")));

        StepVerifier.create(service.approve(1L)).verifyError(ForbiddenException.class);
        StepVerifier.create(service.reject(1L)).verifyError(ForbiddenException.class);
        verifyNoInteractions(requests, gyms);
    }

    @Test
    void requiresExistingPendingRequestBeforeCreatingGym() {
        when(access.requireAdmin()).thenReturn(Mono.just(10L));
        when(requests.findByIdForUpdate(1L)).thenReturn(Mono.empty());
        StepVerifier.create(service.approve(1L)).verifyError(NotFoundException.class);

        GymRequest request = GymRequest.submit(7L, "Gym", "City", "Address");
        request.reject(10L);
        when(requests.findByIdForUpdate(1L)).thenReturn(Mono.just(request));
        StepVerifier.create(service.approve(1L)).verifyError(ConflictException.class);
        verifyNoInteractions(gyms);
    }

    @Test
    void approvalChecksAdminFirstThenCreatesGymAndSavesDecisionInsideTransaction() {
        when(access.requireAdmin()).thenReturn(Mono.just(10L));
        GymRequest request = GymRequest.submit(7L, "Gym", "City", "Address");
        when(requests.findByIdForUpdate(1L)).thenReturn(Mono.just(request));
        when(gyms.existsByCityAndAddress("City", "Address")).thenReturn(Mono.just(false));
        when(gyms.save(any())).thenReturn(Mono.just(new Gym(20L, "Gym", "City", "Address")));
        when(requests.save(request)).thenReturn(Mono.just(request));

        StepVerifier.create(service.approve(1L))
                .assertNext(approved -> {
                    assertThat(approved.getStatus()).isEqualTo(RequestStatus.APPROVED);
                    assertThat(approved.getGymId()).isEqualTo(20L);
                    assertThat(approved.getReviewedBy()).isEqualTo(10L);
                })
                .verifyComplete();
        var order = inOrder(access, transaction, gyms, requests);
        order.verify(access).requireAdmin();
        order.verify(transaction).transactional(any(Mono.class));
        order.verify(gyms).save(any());
        order.verify(requests).save(request);
    }

    @Test
    void decisionSaveFailureIsPropagatedForTransactionRollback() {
        when(access.requireAdmin()).thenReturn(Mono.just(10L));
        GymRequest request = GymRequest.submit(7L, "Gym", "City", "Address");
        when(requests.findByIdForUpdate(1L)).thenReturn(Mono.just(request));
        when(gyms.existsByCityAndAddress("City", "Address")).thenReturn(Mono.just(false));
        when(gyms.save(any())).thenReturn(Mono.just(new Gym(20L, "Gym", "City", "Address")));
        when(requests.save(request)).thenReturn(Mono.error(new IllegalStateException("Ошибка сохранения решения")));

        StepVerifier.create(service.approve(1L)).verifyError(IllegalStateException.class);
    }

    @Test
    void rejectsKnownAddressBeforeWriting() {
        when(access.requireAdmin()).thenReturn(Mono.just(10L));
        when(requests.findByIdForUpdate(1L)).thenReturn(Mono.just(GymRequest.submit(7L, "Gym", "City", "Address")));
        when(gyms.existsByCityAndAddress("City", "Address")).thenReturn(Mono.just(true));

        StepVerifier.create(service.approve(1L)).verifyError(ConflictException.class);
        verify(gyms, never()).save(any());
        verify(requests, never()).save(any());
    }
}
