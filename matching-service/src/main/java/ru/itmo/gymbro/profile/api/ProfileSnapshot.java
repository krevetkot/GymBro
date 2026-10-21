package ru.itmo.gymbro.profile.api;

import java.util.List;

public record ProfileSnapshot(long userId, String name, int age, String about,
                              List<Long> sportIds, List<Long> gymIds) {

    public ProfileSnapshot {
        sportIds = List.copyOf(sportIds);
        gymIds = List.copyOf(gymIds);
    }
}
