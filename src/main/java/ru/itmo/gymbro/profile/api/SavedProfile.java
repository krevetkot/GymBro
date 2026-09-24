package ru.itmo.gymbro.profile.api;

import ru.itmo.gymbro.profile.model.UserProfile;

public class SavedProfile {

    private final UserProfile profile;
    private final boolean created;

    public SavedProfile(UserProfile profile, boolean created) {
        this.profile = profile;
        this.created = created;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public boolean isCreated() {
        return created;
    }
}
