package ru.itmo.gymbro.profile.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class ProfileResponse {

    private final long userId;
    private final String name;
    private final int age;
    private final String about;
    private final List<ProfileSportItem> sports;
    private final List<Long> gymIds;
    private final Instant updatedAt;

    public static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(
                profile.getUserId(),
                profile.getName(),
                profile.getAge(),
                profile.getAbout(),
                profile.getSports().stream()
                        .map(ProfileSportItem::from)
                        .sorted(Comparator.comparing(ProfileSportItem::getSportId))
                        .toList(),
                profile.getGyms().stream()
                        .map(UserGym::getGymId)
                        .sorted()
                        .toList(),
                profile.getUpdatedAt());
    }
}
