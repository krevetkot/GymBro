package ru.itmo.gymbro.profile.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.itmo.gymbro.profile.model.SportLevel;
import ru.itmo.gymbro.profile.model.UserSport;

public class ProfileSportRequest {

    @NotNull
    @Positive
    private Long sportId;

    @NotNull
    private SportLevel level;

    public Long getSportId() {
        return sportId;
    }

    public SportLevel getLevel() {
        return level;
    }

    public UserSport toUserSport() {
        return UserSport.of(sportId, level);
    }
}
