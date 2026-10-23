package ru.itmo.gymbro.matching.dto;

import lombok.Getter;
import ru.itmo.gymbro.profile.api.ProfileSnapshot;

import java.util.List;

@Getter
public class FeedCardResponse {

    private final long userId;
    private final String name;
    private final int age;
    private final String about;
    private final List<Long> sportIds;
    private final List<Long> gymIds;

    public FeedCardResponse(long userId, String name, int age, String about,
                            List<Long> sportIds, List<Long> gymIds) {
        this.userId = userId;
        this.name = name;
        this.age = age;
        this.about = about;
        this.sportIds = List.copyOf(sportIds);
        this.gymIds = List.copyOf(gymIds);
    }

    public static FeedCardResponse from(ProfileSnapshot profile) {
        return new FeedCardResponse(
                profile.userId(),
                profile.name(),
                profile.age(),
                profile.about(),
                profile.sportIds(),
                profile.gymIds());
    }
}
