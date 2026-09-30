package com.example.chookjibupadmin.festival.command.infrastructure;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 재기동 후 누락분과 서울 자정의 자동 상태 전이를 기본 1분 주기로 반영한다. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.festival.progress-sync.enabled", havingValue = "true", matchIfMissing = true)
public class FestivalProgressScheduler {
    private final FestivalService festivalService;

    @Scheduled(fixedDelayString = "${app.festival.progress-sync.delay-millis:60000}")
    public void synchronize() {
        festivalService.synchronizeProgressStatuses();
    }
}
