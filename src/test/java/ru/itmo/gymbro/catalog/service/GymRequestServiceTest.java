package ru.itmo.gymbro.catalog.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import ru.itmo.gymbro.catalog.model.GymRequest;
import ru.itmo.gymbro.catalog.model.RequestStatus;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.catalog.repository.GymRequestRepository;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.ForbiddenException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GymRequestServiceTest {

    private final GymRequestRepository requests = mock(GymRequestRepository.class);
    private final GymRepository gyms = mock(GymRepository.class);
    private final CurrentUserAccess access = mock(CurrentUserAccess.class);
    private final GymRequestService service = new GymRequestService(requests, gyms, access);

    @Test
    void createsPendingRequestWithCurrentAuthorWithoutCreatingGym() {
        when(access.requireActiveUser()).thenReturn(7L);
        when(requests.save(any(GymRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GymRequest request = service.submit(" Gym ", " City ", " Address ");

        assertThat(request.getAuthorId()).isEqualTo(7L);
        assertThat(request.getName()).isEqualTo("Gym");
        assertThat(request.getCity()).isEqualTo("City");
        assertThat(request.getAddress()).isEqualTo("Address");
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(request.getGymId()).isNull();
        assertThat(request.getReviewedBy()).isNull();
        verify(gyms).existsByCityAndAddress("City", "Address");
        verify(gyms, never()).save(any());
    }

    @Test
    void doesNotSaveRequestForExistingGym() {
        when(access.requireActiveUser()).thenReturn(7L);
        when(gyms.existsByCityAndAddress("City", "Address")).thenReturn(true);

        assertThatThrownBy(() -> service.submit("Gym", "City", "Address"))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(requests);
    }

    @Test
    void checksAccessBeforeWritingOrReadingRequests() {
        when(access.requireActiveUser()).thenThrow(new ForbiddenException("Пользователь заблокирован"));
        when(access.requireAdmin()).thenThrow(new ForbiddenException("Нет прав"));

        assertThatThrownBy(() -> service.submit("Gym", "City", "Address"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.getPage(PageRequest.of(0, 20)))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(requests, gyms);
    }
}

