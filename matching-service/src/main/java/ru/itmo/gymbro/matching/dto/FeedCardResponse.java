package ru.itmo.gymbro.matching.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.gymbro.profile.api.ProfileSnapshot;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class FeedCardResponse {

    private final long userId;
    private final String name;
    private final int age;
    private final String about;
    private final List<Long> sportIds;
    private final List<Long> gymIds;
    private final TrainerInfo trainer;

    public static FeedCardResponse from(ProfileSnapshot profile) {
        return new FeedCardResponse(
                profile.userId(),
                profile.name(),
                profile.age(),
                profile.about(),
                List.copyOf(profile.sportIds()),
                List.copyOf(profile.gymIds()),
                TrainerInfo.from(profile.trainer()));
    }
}
