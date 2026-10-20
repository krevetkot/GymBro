package ru.itmo.gymbro.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(example = "{\"email\":\"alex.new@example.com\"}")
public class UpdateUserRequest {

    @Email
    @Pattern(regexp = ".*\\S.*", message = "не должно быть пустым")
    @Size(max = 255)
    private String email;

    @Pattern(regexp = ".*\\S.*", message = "не должно быть пустым")
    @Size(min = 8, max = 72)
    private String password;

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
