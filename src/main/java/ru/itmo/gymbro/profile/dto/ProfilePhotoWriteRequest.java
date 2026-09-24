package ru.itmo.gymbro.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(example = "{\"url\":\"https://example.com/photos/alex.jpg\"}")
public class ProfilePhotoWriteRequest {

    @NotBlank
    @Size(max = 500)
    @Pattern(regexp = "https?://\\S+", message = "должна быть ссылкой http или https без пробелов")
    private String url;

    public String getUrl() {
        return url;
    }
}
