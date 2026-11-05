package ru.itmo.gymbro.shared.security;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Каталог проверяет JWT, выпущенный identity-service, тем же общим секретом.
 * Справочник видов спорта и залов читается без токена, меняет его только ADMIN;
 * заявки на залы рассматривает ADMIN, подаёт — любой пользователь с токеном.
 */
@Configuration
public class JwtSecurityConfiguration {

    public static final String ISSUER = "gymbro-identity";
    public static final String ROLE_CLAIM = "role";

    private static final String[] CATALOG = {"/api/v1/sports", "/api/v1/sports/**", "/api/v1/gyms", "/api/v1/gyms/**"};

    @Bean
    SecretKey jwtSecretKey(@Value("${gymbro.jwt.secret}") String encodedSecret) {
        byte[] key = Base64.getDecoder().decode(encodedSecret);
        if (key.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(SecretKey key) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    /** Claim {@code role} из токена превращается в authority {@code ROLE_<роль>} для {@code hasRole(...)}. */
    static JwtAuthenticationConverter roleAuthorities() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    ProblemDetailSecurityErrors securityErrors(ServerCodecConfigurer codecs) {
        return new ProblemDetailSecurityErrors(codecs);
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ProblemDetailSecurityErrors errors) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, CATALOG).permitAll()
                        .pathMatchers(CATALOG).hasRole("ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/v1/gym-requests").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/v1/gym-requests/*/approve",
                                "/api/v1/gym-requests/*/reject").hasRole("ADMIN")
                        .pathMatchers("/internal/**", "/actuator/health/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new ReactiveJwtAuthenticationConverterAdapter(roleAuthorities())))
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .build();
    }
}
