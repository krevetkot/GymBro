package ru.itmo.gymbro.profile.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import ru.itmo.gymbro.profile.model.SportLevel;
import ru.itmo.gymbro.profile.model.UserSport;

@Getter
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

    public UserSport toUserSport() {
        return UserSport.of(sportId, level);
    }
}
