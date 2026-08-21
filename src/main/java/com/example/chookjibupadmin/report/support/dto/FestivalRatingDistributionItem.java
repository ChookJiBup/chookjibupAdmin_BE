package com.example.chookjibupadmin.report.support.dto;

/**
 * 별점 1~5 분포 한 구간이다.
 */
public record FestivalRatingDistributionItem(
        int rating,
        long count,
        double ratio
) {
}
