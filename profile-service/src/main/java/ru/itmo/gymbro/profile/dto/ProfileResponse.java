package ru.itmo.gymbro.profile.dto;

import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public class ProfileResponse {

    private final long userId;
    private final String name;
    private final int age;
    private final String about;
    private final List<ProfileSportItem> sports;
    private final List<Long> gymIds;
    private final Instant updatedAt;

    public ProfileResponse(long userId, String name, int age, String about,
                           List<ProfileSportItem> sports, List<Long> gymIds, Instant updatedAt) {
        this.userId = userId;
        this.name = name;
        this.age = age;
        this.about = about;
        this.sports = List.copyOf(sports);
        this.gymIds = List.copyOf(gymIds);
        this.updatedAt = updatedAt;
    }

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

    public List<ProfileSportItem> getSports() {
        return sports;
    }

    public List<Long> getGymIds() {
        return gymIds;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
