package ru.itmo.gymbro.profile.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.gymbro.profile.model.UserProfile;

@Getter
@RequiredArgsConstructor
public class SavedProfile {

    private final UserProfile profile;
    private final boolean created;
}
