package ru.itmo.gymbro.profile.api;

import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;

import java.util.List;

public record ProfileSnapshot(long userId, String name, int age, String about,
                              List<Long> sportIds, List<Long> gymIds) {

    public ProfileSnapshot {
        sportIds = List.copyOf(sportIds);
        gymIds = List.copyOf(gymIds);
    }

    public static ProfileSnapshot from(UserProfile profile) {
        return new ProfileSnapshot(
                profile.getUserId(),
                profile.getName(),
                profile.getAge(),
                profile.getAbout(),
                profile.getSports().stream().map(UserSport::getSportId).sorted().toList(),
                profile.getGyms().stream().map(UserGym::getGymId).sorted().toList());
    }
}
