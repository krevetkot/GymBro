package ru.itmo.gymbro.shared.dto;

import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
public class SliceResponse<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final boolean hasNext;

    public SliceResponse(List<T> items, int page, int size, boolean hasNext) {
        this.items = List.copyOf(items);
        this.page = page;
        this.size = size;
        this.hasNext = hasNext;
    }

    public static <T> SliceResponse<T> from(Slice<T> slice) {
        return new SliceResponse<>(slice.getContent(), slice.getNumber(), slice.getSize(), slice.hasNext());
    }
}
