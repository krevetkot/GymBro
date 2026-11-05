package ru.itmo.gymbro.shared.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import ru.itmo.gymbro.shared.api.CurrentUserProvider;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

@Component
class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new UnauthorizedException("Требуется Bearer-токен");
        }
        return parseSubject(jwt.getToken().getSubject());
    }

    private static long parseSubject(String subject) {
        long id;
        try {
            id = Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new UnauthorizedException("Неверный субъект токена");
        }
        if (id <= 0) {
            throw new UnauthorizedException("Неверный субъект токена");
        }
        return id;
    }
}
