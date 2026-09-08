package com.example.chookjibupadmin.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAI 구조화 응답과 저장 Geometry 직렬화에 사용하는 Jackson 2 매퍼이다. */
@Configuration
public class JacksonConfig {
    @Bean
    ObjectMapper objectMapper() {
        // 결과 보고서 지표는 LocalDate를 포함하므로 java.time 모듈이 필요하다.
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
