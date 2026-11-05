package ru.itmo.gymbro.shared.security;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ru.itmo.gymbro.shared.api.ForbiddenException;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

/**
 * Отказы Spring Security (нет токена, токен недействителен, не хватает прав) передаются
 * общему обработчику ошибок и отдаются тем же {@code ProblemDetail}, что и остальные ошибки API.
 */
@RequiredArgsConstructor
class ProblemDetailSecurityErrors implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        resolver.resolveException(request, response, null,
                new UnauthorizedException("Требуется действительный Bearer-токен в заголовке Authorization"));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) {
        resolver.resolveException(request, response, null,
                new ForbiddenException("Недостаточно прав для операции"));
    }
}
