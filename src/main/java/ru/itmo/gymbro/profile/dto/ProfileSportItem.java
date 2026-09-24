package ru.itmo.gymbro.profile.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.itmo.gymbro.profile.model.SportLevel;
import ru.itmo.gymbro.profile.model.UserSport;

public class ProfileSportItem {

    @NotNull
    @Positive
    private Long sportId;

    @NotNull
    private SportLevel level;

    public static ProfileSportItem from(UserSport sport) {
        ProfileSportItem item = new ProfileSportItem();
        item.sportId = sport.getSportId();
        item.level = sport.getLevel();
        return item;
    }

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
