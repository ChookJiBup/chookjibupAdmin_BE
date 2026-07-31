package com.example.chookjibupadmin.festival.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.festival.query.application.dto.FestivalSeriesSearchView;
import com.example.chookjibupadmin.festival.query.repository.FestivalSeriesQueryRepository;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalSeriesQueryServiceTest {

    @InjectMocks
    private FestivalSeriesQueryService queryService;

    @Mock
    private FestivalSeriesQueryRepository queryRepository;

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("공백을 제거한 검색어와 최대 조회 수로 검색한다")
        void success_Search() {
            // given
            FestivalSeriesSearchView view = view();
            given(queryRepository.search("김밥", 10))
                    .willReturn(List.of(view));

            // when
            List<FestivalSeriesSearchView> result =
                    queryService.search(" 김밥 ", 10);

            // then
            assertThat(result).containsExactly(view);
        }

        @Test
        @DisplayName("검색어 길이 100자와 조회 수 20은 허용한다")
        void success_Search_MaxBoundary() {
            // given
            String keyword = "가".repeat(100);
            given(queryRepository.search(keyword, 20))
                    .willReturn(List.of());

            // when
            List<FestivalSeriesSearchView> result =
                    queryService.search(keyword, 20);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 검색어이면 요청 값 예외를 던진다")
        void fail_Search_BlankKeyword_CustomException() {
            // given
            String keyword = " ";

            // when & then
            assertThatThrownBy(() -> queryService.search(keyword, 10))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("검색어가 100자를 초과하면 요청 값 예외를 던진다")
        void fail_Search_KeywordTooLong_CustomException() {
            // given
            String keyword = "가".repeat(101);

            // when & then
            assertThatThrownBy(() -> queryService.search(keyword, 10))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("조회 수가 1 미만이면 요청 값 예외를 던진다")
        void fail_Search_LimitUnderMinimum_CustomException() {
            // given
            int limit = 0;

            // when & then
            assertThatThrownBy(() -> queryService.search("김밥", limit))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("조회 수가 20을 초과하면 요청 값 예외를 던진다")
        void fail_Search_LimitOverMaximum_CustomException() {
            // given
            int limit = 21;

            // when & then
            assertThatThrownBy(() -> queryService.search("김밥", limit))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    private FestivalSeriesSearchView view() {
        return new FestivalSeriesSearchView(
                null, "김밥축제", null, null, null, null, null,
                null, null, null, null
        );
    }
}
