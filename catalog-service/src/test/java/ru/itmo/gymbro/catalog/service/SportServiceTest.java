package ru.itmo.gymbro.catalog.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.itmo.gymbro.catalog.model.Sport;
import ru.itmo.gymbro.catalog.repository.SportRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SportServiceTest {

    private final SportRepository sports = mock(SportRepository.class);
    private final SportService service = new SportService(sports);

    @Test
    void trimsFieldsBeforeDuplicateCheckAndSave() {
        when(sports.existsByName("Sport")).thenReturn(Mono.just(false));
        when(sports.save(any(Sport.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.create(" Sport "))
                .assertNext(created -> assertThat(created.getName()).isEqualTo("Sport"))
                .verifyComplete();
    }

    @Test
    void doesNotSaveDuplicate() {
        when(sports.existsByName("Sport")).thenReturn(Mono.just(true));

        StepVerifier.create(service.create(" Sport ")).verifyError(ConflictException.class);
        verify(sports, never()).save(any());
    }

    @Test
    void invalidNameFailsWithoutTouchingRepository() {
        StepVerifier.create(service.create("  ")).verifyError(IllegalArgumentException.class);
        verify(sports, never()).existsByName(any());
    }

    @Test
    void missingDeleteDoesNotWriteAnything() {
        when(sports.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(1L)).verifyError(NotFoundException.class);
        verify(sports, never()).delete(any(Sport.class));
    }
}
