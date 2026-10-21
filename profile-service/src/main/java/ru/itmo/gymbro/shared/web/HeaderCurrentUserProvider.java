package ru.itmo.gymbro.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.itmo.gymbro.shared.api.CurrentUserProvider;
import ru.itmo.gymbro.shared.api.UnauthorizedException;

import java.util.regex.Pattern;

@Component
class HeaderCurrentUserProvider implements CurrentUserProvider {

    static final String USER_ID_HEADER = "X-User-Id";

    private static final Pattern POSITIVE_ID = Pattern.compile("[1-9]\\d{0,17}");

    @Override
    public long currentUserId() {
        String header = currentRequest().getHeader(USER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw new UnauthorizedException("Не передан заголовок " + USER_ID_HEADER);
        }
        String value = header.trim();
        if (!POSITIVE_ID.matcher(value).matches()) {
            throw new UnauthorizedException(
                    "Заголовок " + USER_ID_HEADER + " должен содержать положительный идентификатор пользователя");
        }
        return Long.parseLong(value);
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        throw new IllegalStateException("Текущий пользователь определяется только внутри HTTP-запроса");
    }
}
