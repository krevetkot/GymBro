package ru.itmo.gymbro.catalog.service;

import org.junit.jupiter.api.Test;
import ru.itmo.gymbro.catalog.model.Gym;
import ru.itmo.gymbro.catalog.repository.GymRepository;
import ru.itmo.gymbro.identity.api.CurrentUserAccess;
import ru.itmo.gymbro.shared.api.ForbiddenException;
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

class GymServiceTest {

    private final GymRepository gyms = mock(GymRepository.class);
    private final CurrentUserAccess access = mock(CurrentUserAccess.class);
    private final GymService service = new GymService(gyms, access);

    @Test
    void deniedChangesNeverAccessTheRepository() {
        when(access.requireAdmin()).thenThrow(new ForbiddenException("Требуется администратор"));
        assertThatThrownBy(() -> service.update(1L, "Gym", "City", "Address"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ForbiddenException.class);
        org.mockito.Mockito.verifyNoInteractions(gyms);
    }

    @Test
    void trimsFieldsBeforeDuplicateCheckAndSave() {
        when(gyms.save(any(Gym.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Gym created = service.create(" Gym ", " City ", " Address ");
        assertThat(created.getName()).isEqualTo("Gym");
        assertThat(created.getCity()).isEqualTo("City");
        assertThat(created.getAddress()).isEqualTo("Address");
        verify(gyms).existsByCityAndAddress("City", "Address");
    }

    @Test
    void doesNotSaveDuplicate() {
        when(gyms.existsByCityAndAddress("City", "Address")).thenReturn(true);
        assertThatThrownBy(() -> service.create(" Gym ", " City ", " Address ")).isInstanceOf(ConflictException.class);
        verify(gyms, never()).save(any());
    }

    @Test
    void allowsUpdateWithoutChangingUniqueFields() {
        when(gyms.findById(1L)).thenReturn(Optional.of(new Gym(1L, "Gym", "City", "Address")));
        when(gyms.save(any(Gym.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Gym updated = service.update(1L, "Gym", "City", "Address");
        assertThat(updated.getId()).isEqualTo(1L);
        verify(gyms).save(updated);
        verify(gyms, never()).existsByCityAndAddress(any(), any());
    }

    @Test
    void missingUpdateAndDeleteDoNotWriteAnything() {
        when(gyms.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(1L, "Gym", "City", "Address")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);
        verify(gyms, never()).save(any());
        verify(gyms, never()).deleteById(anyLong());
    }
}
