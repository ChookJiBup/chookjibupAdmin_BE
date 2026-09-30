package com.example.chookjibupadmin.report.query.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.example.chookjibupadmin.report.support.dto.FestivalReviewMetrics;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class FestivalReviewMetricQueryRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void 리뷰_작성자_표시명을_사용자_닉네임과_방문_유형에_맞게_조회한다() {
        when(jdbcTemplate.queryForList(
                anyString(),
                any(MapSqlParameterSource.class)
        )).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("count(*) as review_count")) {
                return List.of(Map.of(
                        "review_count", 3L,
                        "average_score", 4.0
                ));
            }
            if (sql.contains("group by rating")) {
                return List.of(Map.of(
                        "rating", 4,
                        "rating_count", 3L
                ));
            }
            return List.of(
                    reviewRow(1L, "축제팬", 5, "좋아요"),
                    reviewRow(2L, "현장 방문자", 4, "즐거웠어요"),
                    reviewRow(3L, "탈퇴한 사용자", 3, "무난해요")
            );
        });

        FestivalReviewMetricQueryRepository repository =
                new FestivalReviewMetricQueryRepository(jdbcTemplate);

        FestivalReviewMetrics result = repository.findByFestivalId(10L, null);

        assertThat(result.reviews())
                .extracting(review -> review.displayName())
                .containsExactly("축제팬", "현장 방문자", "탈퇴한 사용자");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate, org.mockito.Mockito.times(3))
                .queryForList(
                        sqlCaptor.capture(),
                        any(MapSqlParameterSource.class)
                );
        assertThat(sqlCaptor.getAllValues().get(2))
                .contains("left join users user_account")
                .contains("when review.user_id is null then '현장 방문자'")
                .contains("when user_account.nickname is null then '탈퇴한 사용자'");
    }

    private Map<String, Object> reviewRow(
            long reviewId,
            String displayName,
            int rating,
            String content
    ) {
        return Map.of(
                "review_id", reviewId,
                "display_name", displayName,
                "rating", rating,
                "content", content
        );
    }
}
