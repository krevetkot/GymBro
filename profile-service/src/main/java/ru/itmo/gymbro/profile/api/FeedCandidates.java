package ru.itmo.gymbro.profile.api;

import java.util.List;

public record FeedCandidates(List<ProfileSnapshot> items, int page, int size, boolean hasNext) {

    public FeedCandidates {
        if (page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("page должен быть >= 0, size — от 1 до 50");
        }
        items = List.copyOf(items);
        if (items.size() > size) {
            throw new IllegalArgumentException("Количество элементов превышает размер страницы");
        }
    }
}
