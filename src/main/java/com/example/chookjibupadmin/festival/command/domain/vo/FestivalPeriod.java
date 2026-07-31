package com.example.chookjibupadmin.festival.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제 운영 날짜 범위를 표현하는 값 객체이다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalPeriod {

    private LocalDate startDate;
    private LocalDate endDate;

    private FestivalPeriod(
            LocalDate startDate,
            LocalDate endDate
    ) {
        validate(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * 시작일과 종료일을 검증한 뒤 축제 기간 값 객체로 변환한다.
     */
    public static FestivalPeriod of(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new FestivalPeriod(startDate, endDate);
    }

    private void validate(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
