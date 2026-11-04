package ru.itmo.gymbro.shared.web;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ru.itmo.gymbro.shared.api.CurrentUserProvider;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

@Component
class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Mono<Long> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .flatMap(authentication -> parseSubject(authentication.getToken().getSubject()))
                .switchIfEmpty(Mono.error(new UnauthorizedException("Требуется Bearer-токен")));
    }

    private Mono<Long> parseSubject(String subject) {
        try {
            long id = Long.parseLong(subject);
            return id > 0 ? Mono.just(id) : Mono.error(new UnauthorizedException("Неверный субъект токена"));
        } catch (NumberFormatException exception) {
            return Mono.error(new UnauthorizedException("Неверный субъект токена"));
        }
    }
}
