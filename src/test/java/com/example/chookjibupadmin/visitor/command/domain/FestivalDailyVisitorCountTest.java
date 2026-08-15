package com.example.chookjibupadmin.visitor.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FestivalDailyVisitorCountTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("일자별 방문 인원 수를 생성한다")
        void success_Create() {
            // given
            LocalDate visitDate = LocalDate.of(2026, 10, 16);

            // when
            FestivalDailyVisitorCount daily = FestivalDailyVisitorCount.create(
                    1L,
                    visitDate,
                    VisitorCount.of(100)
            );

            // then
            assertThat(daily.getFestivalId()).isEqualTo(1L);
            assertThat(daily.getVisitDate()).isEqualTo(visitDate);
            assertThat(daily.getVisitorCountValue()).isEqualTo(100);
        }

        @Test
        @DisplayName("축제 ID가 없으면 생성할 수 없다")
        void fail_Create_NullFestivalId_CustomException() {
            // given

            // when & then
            assertThatThrownBy(() -> FestivalDailyVisitorCount.create(
                    null,
                    LocalDate.of(2026, 10, 16),
                    VisitorCount.of(100)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    @Nested
    @DisplayName("changeVisitorCount")
    class ChangeVisitorCount {

        @Test
        @DisplayName("방문 인원 수를 변경한다")
        void success_ChangeVisitorCount() {
            // given
            FestivalDailyVisitorCount daily = FestivalDailyVisitorCount.create(
                    1L,
                    LocalDate.of(2026, 10, 16),
                    VisitorCount.of(100)
            );

            // when
            daily.changeVisitorCount(VisitorCount.of(250));

            // then
            assertThat(daily.getVisitorCountValue()).isEqualTo(250);
        }

        @Test
        @DisplayName("null 방문 인원 수로 변경할 수 없다")
        void fail_ChangeVisitorCount_Null_CustomException() {
            // given
            FestivalDailyVisitorCount daily = FestivalDailyVisitorCount.create(
                    1L,
                    LocalDate.of(2026, 10, 16),
                    VisitorCount.of(100)
            );

            // when & then
            assertThatThrownBy(() -> daily.changeVisitorCount(null))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
