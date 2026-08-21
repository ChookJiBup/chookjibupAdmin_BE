package com.example.chookjibupadmin.report.support.dto;

/**
 * 결과 보고서에 노출하는 리뷰 요약 항목이다.
 */
public record FestivalReviewItem(
        Long reviewId,
        String displayName,
        Integer rating,
        String content
) {
}
