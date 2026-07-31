package com.example.chookjibupadmin.festival.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalProgressStatus;
import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSearchCondition;
import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSummaryProjection;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class InternalFestivalQueryApplicationServiceTest {

    @Mock
    private InternalFestivalQueryService queryService;

    @Nested
    @DisplayName("searchFestivals")
    class SearchFestivals {

        @Test
        @DisplayName("기본 페이지 조건으로 축제 목록을 조회하고 진행 상태를 계산한다")
        void success_SearchFestivals() {
            // given
            InternalFestivalQueryApplicationService service = service();
            InternalFestivalSearchCondition condition =
                    new InternalFestivalSearchCondition(
                            InternalFestivalProgressStatus.ONGOING,
                            "새우젓",
                            LocalDate.of(2026, 10, 10)
                    );
            PageRequest pageable = PageRequest.of(0, 20);
            given(queryService.searchFestivals(condition, pageable))
                    .willReturn(new PageImpl<>(List.of(projection()), pageable, 1));

            // when
            var result = service.searchFestivals(
                    InternalFestivalProgressStatus.ONGOING,
                    " 새우젓 ",
                    null,
                    null
            );

            // then
            assertThat(result.getContent())
                    .extracting("progressStatus")
                    .containsExactly(InternalFestivalProgressStatus.ONGOING);
            then(queryService).should().searchFestivals(condition, pageable);
        }

        @Test
        @DisplayName("페이지 번호와 크기를 지정해 조회한다")
        void success_SearchFestivals_ByPage() {
            // given
            InternalFestivalQueryApplicationService service = service();
            InternalFestivalSearchCondition condition =
                    new InternalFestivalSearchCondition(
                            null,
                            null,
                            LocalDate.of(2026, 10, 10)
                    );
            PageRequest pageable = PageRequest.of(1, 100);
            given(queryService.searchFestivals(condition, pageable))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            // when
            var result = service.searchFestivals(null, null, 1, 100);

            // then
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("음수 페이지면 CustomException을 던진다")
        void fail_SearchFestivals_CustomException_NegativePage() {
            // given
            InternalFestivalQueryApplicationService service = service();

            // when & then
            assertThatThrownBy(() -> service.searchFestivals(null, null, -1, 20))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("페이지 크기가 1보다 작으면 CustomException을 던진다")
        void fail_SearchFestivals_CustomException_SizeUnderBoundary() {
            // given
            InternalFestivalQueryApplicationService service = service();

            // when & then
            assertThatThrownBy(() -> service.searchFestivals(null, null, 0, 0))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("페이지 크기가 최대값을 넘으면 CustomException을 던진다")
        void fail_SearchFestivals_CustomException_SizeOverBoundary() {
            // given
            InternalFestivalQueryApplicationService service = service();

            // when & then
            assertThatThrownBy(() -> service.searchFestivals(null, null, 0, 101))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    private InternalFestivalQueryApplicationService service() {
        return new InternalFestivalQueryApplicationService(
                queryService,
                Clock.fixed(
                        Instant.parse("2026-10-10T00:00:00Z"),
                        ZoneId.of("Asia/Seoul")
                )
        );
    }

    private InternalFestivalSummaryProjection projection() {
        return new InternalFestivalSummaryProjection(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "마포나루 새우젓축제",
                "지역 축제 설명",
                "서울특별시 마포구 월드컵로 243",
                "월드컵공원",
                2026,
                LocalDate.of(2026, 10, 9),
                LocalDate.of(2026, 10, 18),
                LocalTime.of(10, 0),
                LocalTime.of(21, 0)
        );
    }
}
