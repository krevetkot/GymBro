package ru.itmo.gymbro.profile.dto;

public class ProfilePhotoResponse {

    private final int position;
    private final String url;

    public ProfilePhotoResponse(int position, String url) {
        this.position = position;
        this.url = url;
    }

    public int getPosition() {
        return position;
    }

    public String getUrl() {
        return url;
    }
}
