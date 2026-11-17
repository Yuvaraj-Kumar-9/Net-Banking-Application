package com.hfdcbank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger/OpenAPI metadata and the two supported authentication schemes. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hfdcOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HFDC Bank API")
                        .version("1.0.0")
                        .description("Fictional educational banking simulation. "
                                + "Use the browser UI (HttpOnly cookie + CSRF header) or a "
                                + "Bearer token for API tools."))
                .components(new Components()
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("hfdc_token"))
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
