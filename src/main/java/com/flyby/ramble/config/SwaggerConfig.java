package com.flyby.ramble.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Ramble REST API",
                description = "Ramble REST API 문서",
                version = "v0.0.1"
        )
)
@Configuration
public class SwaggerConfig {
}
