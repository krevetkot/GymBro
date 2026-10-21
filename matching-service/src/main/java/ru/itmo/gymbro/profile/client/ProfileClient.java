package ru.itmo.gymbro.profile.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itmo.gymbro.profile.api.FeedCandidates;

import java.util.Set;

@FeignClient(name = "profile-service")
public interface ProfileClient {

    @GetMapping("/internal/v1/profiles/feed")
    FeedCandidates findFeedCandidates(@RequestParam("viewerUserId") long viewerUserId,
                                      @RequestParam("excludedUserIds") Set<Long> excludedUserIds,
                                      @RequestParam("page") int page,
                                      @RequestParam("size") int size);
}
