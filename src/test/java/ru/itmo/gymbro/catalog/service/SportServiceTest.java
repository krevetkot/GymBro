package ru.itmo.gymbro.catalog.service;

import org.junit.jupiter.api.Test;
import ru.itmo.gymbro.catalog.model.Sport;
import ru.itmo.gymbro.catalog.repository.SportRepository;
import ru.itmo.gymbro.shared.api.ConflictException;
import ru.itmo.gymbro.shared.api.NotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SportServiceTest {

    private final SportRepository sports = mock(SportRepository.class);
    private final SportService service = new SportService(sports);

    @Test
    void trimsFieldsBeforeDuplicateCheckAndSave() {
        when(sports.save(any(Sport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Sport created = service.create(" Sport ");
        assertThat(created.getName()).isEqualTo("Sport");
        verify(sports).existsByName("Sport");
    }

    @Test
    void doesNotSaveDuplicate() {
        when(sports.existsByName("Sport")).thenReturn(true);
        assertThatThrownBy(() -> service.create(" Sport ")).isInstanceOf(ConflictException.class);
        verify(sports, never()).save(any());
    }

    @Test
    void allowsUpdateWithoutChangingUniqueFields() {
        when(sports.findById(1L)).thenReturn(Optional.of(new Sport(1L, "Sport")));
        when(sports.save(any(Sport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Sport updated = service.update(1L, "Sport");
        assertThat(updated.getId()).isEqualTo(1L);
        verify(sports).save(updated);
        verify(sports, never()).existsByName(any());
    }

    @Test
    void missingUpdateAndDeleteDoNotWriteAnything() {
        when(sports.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(1L, "Sport")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);
        verify(sports, never()).save(any());
        verify(sports, never()).deleteById(anyLong());
    }
}
