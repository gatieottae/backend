package com.gatieottae.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 인증 스키마 설정
 * - "Authorize" 버튼을 띄우기 위한 JWT Bearer 스키마 등록
 */
@OpenAPIDefinition( // ← 추가
        security = { @SecurityRequirement(name = "bearerAuth") }
)
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}