package ru.itmo.gymbro.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(example = "{\"sportId\":1,\"fileUrl\":\"https://files.example.com/diplomas/coach-123.pdf\"}")
public record SubmitCertificateRequest(
        @NotNull @Positive Long sportId,
        @Schema(description = "Ссылка на скан сертификата или диплома; загрузка файлов появится вместе с файловым сервисом")
        @NotBlank @Size(max = 500) @Pattern(regexp = "https?://\\S+", message = "должна быть ссылкой http(s)")
        String fileUrl) {
}
