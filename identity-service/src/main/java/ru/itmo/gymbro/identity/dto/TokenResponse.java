package ru.itmo.gymbro.identity.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
