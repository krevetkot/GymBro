package ru.itmo.gymbro.identity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.identity.dto.TokenResponse;
import ru.itmo.gymbro.identity.repository.UserRepository;
import ru.itmo.gymbro.identity.security.JwtTokenService;
import ru.itmo.gymbro.shared.api.UnauthorizedException;
import ru.itmo.gymbro.shared.persistence.BlockingJpa;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final BlockingJpa jpa;
    private final JwtTokenService tokens;

    public Mono<TokenResponse> login(String email, String password) {
        return jpa.read(() -> users.findByEmail(email).orElse(null))
                .filter(user -> user.isActive() && passwords.matches(password, user.getPassword()))
                .switchIfEmpty(Mono.error(new UnauthorizedException("Неверный email или пароль")))
                .map(tokens::issue);
    }
}
