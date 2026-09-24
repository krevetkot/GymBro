package ru.itmo.gymbro.profile.dto;

import ru.itmo.gymbro.profile.model.UserGym;
import ru.itmo.gymbro.profile.model.UserPhoto;
import ru.itmo.gymbro.profile.model.UserProfile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class ProfileResponse {

    private final long userId;
    private final String name;
    private final int age;
    private final String about;
    private final List<ProfilePhotoResponse> photos;
    private final List<ProfileSportResponse> sports;
    private final List<Long> gymIds;
    private final Instant updatedAt;

    public ProfileResponse(long userId, String name, int age, String about,
                           List<ProfilePhotoResponse> photos, List<ProfileSportResponse> sports,
                           List<Long> gymIds, Instant updatedAt) {
        this.userId = userId;
        this.name = name;
        this.age = age;
        this.about = about;
        this.photos = List.copyOf(photos);
        this.sports = List.copyOf(sports);
        this.gymIds = List.copyOf(gymIds);
        this.updatedAt = updatedAt;
    }

    public static ProfileResponse from(UserProfile profile) {
        List<UserPhoto> photos = profile.getPhotos();
        return new ProfileResponse(
                profile.getUserId(),
                profile.getName(),
                profile.getAge(),
                profile.getAbout(),
                IntStream.range(0, photos.size())
                        .mapToObj(position -> new ProfilePhotoResponse(position, photos.get(position).getUrl()))
                        .toList(),
                profile.getSports().stream()
                        .map(ProfileSportResponse::from)
                        .sorted(Comparator.comparingLong(ProfileSportResponse::getSportId))
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

    public List<ProfilePhotoResponse> getPhotos() {
        return photos;
    }

    public List<ProfileSportResponse> getSports() {
        return sports;
    }

    public List<Long> getGymIds() {
        return gymIds;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
