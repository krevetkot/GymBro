package ru.itmo.gymbro.profile.api;

import lombok.Getter;
import ru.itmo.gymbro.profile.model.UserProfile;

@Getter
public class SavedProfile {

    private final UserProfile profile;
    private final boolean created;

    public SavedProfile(UserProfile profile, boolean created) {
        this.profile = profile;
        this.created = created;
    }
}
