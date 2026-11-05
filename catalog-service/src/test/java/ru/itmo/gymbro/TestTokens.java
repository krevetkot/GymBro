package ru.itmo.gymbro;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Выпускает JWT так же, как identity-service, но тестовым секретом. Роль в токене берётся из
 * {@link TestUsers} — как identity кладёт в токен роль пользователя на момент входа.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestTokens {

    public static final String SECRET = "9VoyJYlyob2NakCY+RZD/RsOg8ImpSVI4ENBgpmvi/U=";
    private static final String FOREIGN_SECRET = "s1y8n2C4bXQx0b0WvJq3l6Fv2rS8hT1mZ5uK9pD7eGc=";
    private static final String ISSUER = "gymbro-identity";
    private static final Map<Long, String> ROLES = new ConcurrentHashMap<>();

    static void rememberRole(long userId, String role) {
        ROLES.put(userId, role);
    }

    public static String bearer(long userId) {
        return bearer(userId, ROLES.getOrDefault(userId, "USER"));
    }

    /** Токен с явно указанной ролью: например, выданный до того, как роль сменили. */
    public static String bearer(long userId, String role) {
        return bearer(Long.toString(userId), role);
    }

    /** Подписанный токен с произвольным {@code sub}; роль — как у пользователя с таким id, иначе USER. */
    public static String bearer(String subject) {
        String role = subject.matches("[1-9][0-9]{0,17}") ? ROLES.getOrDefault(Long.parseLong(subject), "USER") : "USER";
        return bearer(subject, role);
    }

    /**
     * Подписанный токен с произвольными {@code sub} и ролью: например, «админский» токен с несуществующим
     * или некорректным субъектом — фильтр по роли его пропустит, а сервис должен отказать (401).
     */
    public static String bearer(String subject, String role) {
        return "Bearer " + token(SECRET, ISSUER, subject, role, Instant.now(), Instant.now().plusSeconds(3600));
    }

    public static String expired(long userId) {
        Instant past = Instant.now().minusSeconds(7200);
        return "Bearer " + token(SECRET, ISSUER, Long.toString(userId), "USER", past, past.plusSeconds(3600));
    }

    public static String foreignSignature(long userId) {
        return "Bearer " + token(FOREIGN_SECRET, ISSUER, Long.toString(userId), "ADMIN", Instant.now(), Instant.now().plusSeconds(3600));
    }

    public static String foreignIssuer(long userId) {
        return "Bearer " + token(SECRET, "someone-else", Long.toString(userId), "ADMIN", Instant.now(), Instant.now().plusSeconds(3600));
    }

    private static String token(String secret, String issuer, String subject, String role,
                                Instant issuedAt, Instant expiresAt) {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
