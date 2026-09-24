package ru.itmo.gymbro.matching.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.gymbro.matching.api.LikeUserUseCase;
import ru.itmo.gymbro.matching.dto.LikeRequest;
import ru.itmo.gymbro.matching.dto.LikeResponse;

@RestController
@RequestMapping("/api/v1/likes")
class LikeController {

    private final LikeUserUseCase likeUser;

    LikeController(LikeUserUseCase likeUser) {
        this.likeUser = likeUser;
    }

    @PostMapping
    ResponseEntity<LikeResponse> like(@Valid @RequestBody LikeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LikeResponse.from(likeUser.like(request.getToUserId())));
    }
}
