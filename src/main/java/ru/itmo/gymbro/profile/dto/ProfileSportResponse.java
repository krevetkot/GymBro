package ru.itmo.gymbro.profile.dto;

import ru.itmo.gymbro.profile.model.SportLevel;
import ru.itmo.gymbro.profile.model.UserSport;

public class ProfileSportResponse {

    private final long sportId;
    private final SportLevel level;

    public ProfileSportResponse(long sportId, SportLevel level) {
        this.sportId = sportId;
        this.level = level;
    }

    public static ProfileSportResponse from(UserSport sport) {
        return new ProfileSportResponse(sport.getSportId(), sport.getLevel());
    }

    public long getSportId() {
        return sportId;
    }

    public SportLevel getLevel() {
        return level;
    }
}
