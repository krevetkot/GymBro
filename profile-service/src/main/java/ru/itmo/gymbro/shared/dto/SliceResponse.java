package ru.itmo.gymbro.shared.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class SliceResponse<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final boolean hasNext;

    public static <T> SliceResponse<T> from(Slice<T> slice) {
        return new SliceResponse<>(List.copyOf(slice.getContent()), slice.getNumber(), slice.getSize(), slice.hasNext());
    }
}
