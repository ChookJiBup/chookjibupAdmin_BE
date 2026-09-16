package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 사전 계획과 현재 관측 모두 종료 후 일반 변경을 차단한다. */
@Component
@RequiredArgsConstructor
public class QueueWriteAccess {
    private final FestivalService festivalService;
    private final Clock clock;
    public void requireOpen(UUID festivalId) {
        var festival = festivalService.getByPublicId(festivalId);
        if (festival.getEndDate() == null || LocalDate.now(clock).isAfter(festival.getEndDate())) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_OPERATION_CLOSED);
        }
    }
}
