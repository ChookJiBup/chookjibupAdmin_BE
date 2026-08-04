package com.example.chookjibupadmin.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAI 구조화 응답과 저장 Geometry 직렬화에 사용하는 Jackson 2 매퍼이다. */
@Configuration
public class JacksonConfig {
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
