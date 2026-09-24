package ru.itmo.gymbro.matching.dto;

import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;

import java.util.List;

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

    public static FeedCardResponse from(UserProfile profile) {
        return new FeedCardResponse(
                profile.getUserId(),
                profile.getName(),
                profile.getAge(),
                profile.getAbout(),
                profile.getSports().stream().map(UserSport::getSportId).sorted().toList(),
                profile.getGyms().stream().map(UserGym::getGymId).sorted().toList());
    }

    public long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getAbout() {
        return about;
    }

    public List<Long> getSportIds() {
        return sportIds;
    }

    public List<Long> getGymIds() {
        return gymIds;
    }
}
