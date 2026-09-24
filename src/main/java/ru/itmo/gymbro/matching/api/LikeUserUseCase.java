package ru.itmo.gymbro.matching.api;

public interface LikeUserUseCase {

    LikeOutcome like(long toUserId);
}
