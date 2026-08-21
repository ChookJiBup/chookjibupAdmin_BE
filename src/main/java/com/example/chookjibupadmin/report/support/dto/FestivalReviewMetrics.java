package com.example.chookjibupadmin.report.support.dto;

/**
 * 리뷰 집계 결과이다. 원본 리뷰 Entity 없이 조회 전용으로 사용한다.
 */
public record FestivalReviewMetrics(
        boolean available,
        Double averageScore,
        Double previousAverageScore,
        Double scoreDelta,
        long reviewCount,
        java.util.List<FestivalRatingDistributionItem> ratingDistribution,
        java.util.List<FestivalReviewItem> featuredReviews,
        java.util.List<FestivalReviewItem> reviews,
        boolean hasMore
) {

    public static FestivalReviewMetrics empty() {
        return new FestivalReviewMetrics(
                false,
                null,
                null,
                null,
                0L,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                false
        );
    }
}
