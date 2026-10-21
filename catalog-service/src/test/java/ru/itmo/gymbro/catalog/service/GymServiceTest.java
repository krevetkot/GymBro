package ru.itmo.gymbro.catalog.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GymServiceTest {

    private final GymRepository gyms = mock(GymRepository.class);
    private final CurrentUserAccess access = mock(CurrentUserAccess.class);
    private final GymService service = new GymService(gyms, access);

    @Test
    void deniedChangesNeverAccessTheRepository() {
        when(access.requireAdmin()).thenReturn(Mono.error(new ForbiddenException("Требуется администратор")));

        StepVerifier.create(service.update(1L, "Gym", "City", "Address")).verifyError(ForbiddenException.class);
        StepVerifier.create(service.delete(1L)).verifyError(ForbiddenException.class);
        verifyNoInteractions(gyms);
    }

    @Test
    void trimsFieldsBeforeDuplicateCheckAndSave() {
        when(gyms.existsByCityAndAddress("City", "Address")).thenReturn(Mono.just(false));
        when(gyms.save(any(Gym.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.create(" Gym ", " City ", " Address "))
                .assertNext(created -> {
                    assertThat(created.getName()).isEqualTo("Gym");
                    assertThat(created.getCity()).isEqualTo("City");
                    assertThat(created.getAddress()).isEqualTo("Address");
                })
                .verifyComplete();
    }

    @Test
    void doesNotSaveDuplicate() {
        when(gyms.existsByCityAndAddress("City", "Address")).thenReturn(Mono.just(true));

        StepVerifier.create(service.create(" Gym ", " City ", " Address ")).verifyError(ConflictException.class);
        verify(gyms, never()).save(any());
    }

    @Test
    void allowsUpdateWithoutChangingUniqueFields() {
        when(access.requireAdmin()).thenReturn(Mono.just(10L));
        when(gyms.findById(1L)).thenReturn(Mono.just(new Gym(1L, "Gym", "City", "Address")));
        when(gyms.save(any(Gym.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.update(1L, "New name", "City", "Address"))
                .assertNext(updated -> {
                    assertThat(updated.getId()).isEqualTo(1L);
                    assertThat(updated.getName()).isEqualTo("New name");
                })
                .verifyComplete();
        verify(gyms, never()).existsByCityAndAddress(any(), any());
    }

    @Test
    void rejectsMovingToOccupiedAddress() {
        when(access.requireAdmin()).thenReturn(Mono.just(10L));
        when(gyms.findById(1L)).thenReturn(Mono.just(new Gym(1L, "Gym", "City", "Address")));
        when(gyms.existsByCityAndAddress("City", "Other")).thenReturn(Mono.just(true));

        StepVerifier.create(service.update(1L, "Gym", "City", "Other")).verifyError(ConflictException.class);
        verify(gyms, never()).save(any());
    }

    @Test
    void missingUpdateAndDeleteDoNotWriteAnything() {
        when(access.requireAdmin()).thenReturn(Mono.just(10L));
        when(gyms.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(service.update(1L, "Gym", "City", "Address")).verifyError(NotFoundException.class);
        StepVerifier.create(service.delete(1L)).verifyError(NotFoundException.class);
        verify(gyms, never()).save(any());
        verify(gyms, never()).delete(any(Gym.class));
    }
}
