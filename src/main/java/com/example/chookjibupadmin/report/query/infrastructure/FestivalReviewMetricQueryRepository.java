package com.example.chookjibupadmin.report.query.infrastructure;

import com.example.chookjibupadmin.report.support.dto.FestivalRatingDistributionItem;
import com.example.chookjibupadmin.report.support.dto.FestivalReviewItem;
import com.example.chookjibupadmin.report.support.dto.FestivalReviewMetrics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 사용자 리뷰 원본을 Entity 없이 집계 조회한다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FestivalReviewMetricQueryRepository {

    private static final int REVIEW_LIMIT = 50;
    private static final String DISPLAY_NAME = "방문객";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FestivalReviewMetrics findByFestivalId(
            Long festivalId,
            Long previousFestivalId
    ) {
        try {
            ReviewAggregate aggregate = findAggregate(festivalId);
            if (aggregate.reviewCount() == 0L) {
                return FestivalReviewMetrics.empty();
            }

            Map<Integer, Long> ratingCounts = findRatingDistribution(festivalId);
            long scored = ratingCounts.values().stream()
                    .mapToLong(Long::longValue)
                    .sum();
            List<FestivalRatingDistributionItem> distribution = new ArrayList<>();
            for (int rating = 1; rating <= 5; rating++) {
                long count = ratingCounts.getOrDefault(rating, 0L);
                double ratio = scored == 0L ? 0d : (double) count / scored;
                distribution.add(new FestivalRatingDistributionItem(
                        rating,
                        count,
                        ratio
                ));
            }

            List<FestivalReviewItem> reviews = findRecentReviews(festivalId);
            boolean hasMore = aggregate.reviewCount() > REVIEW_LIMIT;
            Double previousAverage = previousFestivalId == null
                    ? null
                    : findAverageScore(previousFestivalId);
            Double delta = aggregate.averageScore() == null
                    || previousAverage == null
                    ? null
                    : aggregate.averageScore() - previousAverage;

            return new FestivalReviewMetrics(
                    true,
                    aggregate.averageScore(),
                    previousAverage,
                    delta,
                    aggregate.reviewCount(),
                    distribution,
                    pickFeatured(reviews),
                    reviews,
                    hasMore
            );
        } catch (RuntimeException exception) {
            log.debug(
                    "festival_review aggregation skipped: {}",
                    exception.getMessage()
            );
            return FestivalReviewMetrics.empty();
        }
    }

    private ReviewAggregate findAggregate(Long festivalId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                    select count(*) as review_count,
                           avg(rating) as average_score
                    from festival_review
                    where festival_id = :festivalId
                    """,
                new MapSqlParameterSource("festivalId", festivalId)
        );
        if (rows.isEmpty()) {
            return new ReviewAggregate(0L, null);
        }
        Map<String, Object> row = rows.get(0);
        long reviewCount = toLong(row.get("review_count"));
        Double average = row.get("average_score") == null
                ? null
                : ((Number) row.get("average_score")).doubleValue();
        return new ReviewAggregate(reviewCount, average);
    }

    private Map<Integer, Long> findRatingDistribution(Long festivalId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                    select rating, count(*) as rating_count
                    from festival_review
                    where festival_id = :festivalId
                      and rating between 1 and 5
                    group by rating
                    """,
                new MapSqlParameterSource("festivalId", festivalId)
        );
        Map<Integer, Long> counts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer rating = toInteger(row.get("rating"));
            if (rating != null) {
                counts.put(rating, toLong(row.get("rating_count")));
            }
        }
        return counts;
    }

    private List<FestivalReviewItem> findRecentReviews(Long festivalId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                    select review_id, rating, content
                    from festival_review
                    where festival_id = :festivalId
                    order by created_at desc
                    limit :limit
                    """,
                new MapSqlParameterSource()
                        .addValue("festivalId", festivalId)
                        .addValue("limit", REVIEW_LIMIT)
        );
        List<FestivalReviewItem> reviews = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            reviews.add(new FestivalReviewItem(
                    toLong(row.get("review_id")),
                    DISPLAY_NAME,
                    toInteger(row.get("rating")),
                    (String) row.get("content")
            ));
        }
        return reviews;
    }

    private Double findAverageScore(Long festivalId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                    select avg(rating) as average_score
                    from festival_review
                    where festival_id = :festivalId
                      and rating is not null
                    """,
                new MapSqlParameterSource("festivalId", festivalId)
        );
        if (rows.isEmpty() || rows.get(0).get("average_score") == null) {
            return null;
        }
        return ((Number) rows.get(0).get("average_score")).doubleValue();
    }

    private List<FestivalReviewItem> pickFeatured(
            List<FestivalReviewItem> reviews
    ) {
        List<FestivalReviewItem> positive = reviews.stream()
                .filter(item -> item.rating() != null && item.rating() >= 4)
                .limit(2)
                .toList();
        List<FestivalReviewItem> negative = reviews.stream()
                .filter(item -> item.rating() != null && item.rating() <= 2)
                .limit(1)
                .toList();
        List<FestivalReviewItem> featured = new ArrayList<>();
        featured.addAll(positive);
        featured.addAll(negative);
        if (featured.size() >= 3) {
            return featured.subList(0, 3);
        }
        for (FestivalReviewItem review : reviews) {
            if (featured.size() >= 3) {
                break;
            }
            if (!featured.contains(review)) {
                featured.add(review);
            }
        }
        return featured;
    }

    private Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private Long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private record ReviewAggregate(long reviewCount, Double averageScore) {
    }
}
