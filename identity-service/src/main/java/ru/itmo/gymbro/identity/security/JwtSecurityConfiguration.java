package ru.itmo.gymbro.identity.security;

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
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
public class JwtSecurityConfiguration {

    public static final String ISSUER = "gymbro-identity";
    public static final String ROLE_CLAIM = "role";

    @Bean
    SecretKey jwtSecretKey(@Value("${gymbro.jwt.secret}") String encodedSecret) {
        byte[] key = Base64.getDecoder().decode(encodedSecret);
        if (key.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
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
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/users").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/users/*/role").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/v1/certificates").hasAnyRole("USER", "TRAINER")
                        .pathMatchers(HttpMethod.GET, "/api/v1/certificates").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/v1/certificates/*/approve",
                                "/api/v1/certificates/*/reject").hasRole("ADMIN")
                        .pathMatchers("/internal/v1/users/**", "/actuator/health/**",
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
