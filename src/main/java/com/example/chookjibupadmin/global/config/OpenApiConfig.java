package com.example.chookjibupadmin.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 문서의 기본 정보와 Bearer JWT 보안 스키마를 설정한다.
 */
@Configuration
public class OpenApiConfig {

    /**
     * 관리자 API 문서에서 사용할 OpenAPI 설정을 생성한다.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .info(new Info()
                        .title("ChookJiBup Admin API")
                        .version("v1")
                        .description("관리자 회원가입, 로그인, 권한 API"));
    }
}
