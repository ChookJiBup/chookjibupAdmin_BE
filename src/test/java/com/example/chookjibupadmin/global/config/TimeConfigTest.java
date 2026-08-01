package com.example.chookjibupadmin.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimeConfigTest {

    @Test
    @DisplayName("애플리케이션 Clock은 서울 시간대를 사용한다")
    void success_Clock_AsiaSeoul() {
        // given
        TimeConfig timeConfig = new TimeConfig();

        // when
        Clock clock = timeConfig.clock();

        // then
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
    }
}
