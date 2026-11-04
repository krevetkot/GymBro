package ru.itmo.gymbro.identity.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.identity.dto.LoginRequest;
import ru.itmo.gymbro.identity.dto.TokenResponse;
import ru.itmo.gymbro.identity.service.LoginService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Авторизация")
@RequiredArgsConstructor
class AuthController {

    private final LoginService loginService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Войти по email и паролю")
    Mono<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request.email(), request.password());
    }
}
