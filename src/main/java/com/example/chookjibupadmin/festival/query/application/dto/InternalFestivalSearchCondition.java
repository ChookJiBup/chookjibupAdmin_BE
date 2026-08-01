package com.example.chookjibupadmin.festival.query.application.dto;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import java.time.LocalDate;

/**
 * 사용자 서버용 축제 목록 조회 조건이다.
 */
public record InternalFestivalSearchCondition(
        FestivalProgressStatus progressStatus,
        String keyword,
        LocalDate today
) {

    public InternalFestivalSearchCondition {
        if (today == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        keyword = normalizeKeyword(keyword);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase();
    }
}
