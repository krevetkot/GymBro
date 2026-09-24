package ru.itmo.gymbro.profile.api;

import ru.itmo.gymbro.profile.model.UserProfile;
import ru.itmo.gymbro.profile.model.UserSport;

import java.util.List;

public interface ChangeProfileSportsUseCase {

    UserProfile replaceMySports(List<UserSport> sports);
}
