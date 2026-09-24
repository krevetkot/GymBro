package ru.itmo.gymbro.matching.api;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import ru.itmo.gymbro.profile.model.UserProfile;

public interface BrowseFeedUseCase {

    Slice<UserProfile> nextCards(Pageable pageable);
}
