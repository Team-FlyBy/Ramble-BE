package com.flyby.ramble.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@OpenAPIDefinition(
        info = @Info(
                title = "Ramble REST API",
                description = "Ramble REST API 문서",
                version = "v0.0.1"
        )
)
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI swaggerApi() {
        Components components = new Components()
            .addSecuritySchemes(HttpHeaders.AUTHORIZATION, new SecurityScheme()
                .name(HttpHeaders.AUTHORIZATION)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .description("JWT 토큰 정보"));

        return new OpenAPI().components(components);
    }

    @Bean
    public GroupedOpenApi authGroup() {
        return GroupedOpenApi.builder()
            .group("auth")
            .pathsToMatch("/oauth/**", "/auth/**")
            .build();
    }

}
