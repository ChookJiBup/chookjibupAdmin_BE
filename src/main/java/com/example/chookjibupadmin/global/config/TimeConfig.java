package com.example.chookjibupadmin.global.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 시간 기반 정책 검증에 사용할 시스템 Clock을 제공한다.
 */
@Configuration
public class TimeConfig {

    /**
     * 애플리케이션 기본 시간 기준을 반환한다.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
