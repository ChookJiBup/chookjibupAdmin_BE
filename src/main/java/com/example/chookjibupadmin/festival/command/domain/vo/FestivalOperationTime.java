package com.example.chookjibupadmin.festival.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제 일일 운영 시간 범위를 표현하는 값 객체이다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalOperationTime {

    private LocalTime startTime;
    private LocalTime endTime;

    private FestivalOperationTime(
            LocalTime startTime,
            LocalTime endTime
    ) {
        validate(startTime, endTime);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * 시작 시간과 종료 시간을 검증한 뒤 운영 시간 값 객체로 변환한다.
     */
    public static FestivalOperationTime of(
            LocalTime startTime,
            LocalTime endTime
    ) {
        return new FestivalOperationTime(startTime, endTime);
    }

    private void validate(
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
