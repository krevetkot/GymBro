package ru.itmo.gymbro.identity.security;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import ru.itmo.gymbro.identity.dto.TokenResponse;
import ru.itmo.gymbro.identity.model.User;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);

    private final JwtEncoder encoder;

    public TokenResponse issue(User user) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtSecurityConfiguration.ISSUER)
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(TOKEN_LIFETIME))
                .claim(JwtSecurityConfiguration.ROLE_CLAIM, user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", TOKEN_LIFETIME.toSeconds());
    }
}
