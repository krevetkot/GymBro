package ru.itmo.gymbro;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Выпускает JWT так же, как identity-service, но тестовым секретом.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestTokens {

    public static final String SECRET = "9VoyJYlyob2NakCY+RZD/RsOg8ImpSVI4ENBgpmvi/U=";
    private static final String FOREIGN_SECRET = "s1y8n2C4bXQx0b0WvJq3l6Fv2rS8hT1mZ5uK9pD7eGc=";
    private static final String ISSUER = "gymbro-identity";

    public static String bearer(long userId) {
        return bearer(Long.toString(userId));
    }

    /** Подписанный токен с произвольным {@code sub}: для проверок некорректного субъекта. */
    public static String bearer(String subject) {
        return "Bearer " + token(SECRET, ISSUER, subject, Instant.now(), Instant.now().plusSeconds(3600));
    }

    public static String expired(long userId) {
        Instant past = Instant.now().minusSeconds(7200);
        return "Bearer " + token(SECRET, ISSUER, Long.toString(userId), past, past.plusSeconds(3600));
    }

    public static String foreignSignature(long userId) {
        return "Bearer " + token(FOREIGN_SECRET, ISSUER, Long.toString(userId), Instant.now(), Instant.now().plusSeconds(3600));
    }

    public static String foreignIssuer(long userId) {
        return "Bearer " + token(SECRET, "someone-else", Long.toString(userId), Instant.now(), Instant.now().plusSeconds(3600));
    }

    private static String token(String secret, String issuer, String subject, Instant issuedAt, Instant expiresAt) {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
