package ru.itmo.gymbro.shared.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfiguration {

    static final String CURRENT_USER_SCHEME = "currentUser";

    @Bean
    OpenAPI gymBroOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("GymBro API")
                        .version("v1")
                        .description("Поиск партнёров для совместных тренировок: пользователи, "
                                + "каталог видов спорта и залов, анкеты, лайки и мэтчи. "
                                + "Списки отдаются постранично, не больше 50 записей за запрос."))
                .components(new Components()
                        .addSecuritySchemes(CURRENT_USER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(HeaderCurrentUserProvider.USER_ID_HEADER)
                                .description("Идентификатор текущего пользователя. "
                                        + "Временная замена JWT до появления spring-security")))
                .addSecurityItem(new SecurityRequirement().addList(CURRENT_USER_SCHEME));
    }
}
