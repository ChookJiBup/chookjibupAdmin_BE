package com.example.chookjibupadmin.festival.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FestivalPeriodTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("JPA 기본 생성자로 생성할 수 있다")
        void success_Constructor_ForJpa() {
            // given

            // when
            FestivalPeriod period = new FestivalPeriod();

            // then
            assertThat(period.getStartDate()).isNull();
            assertThat(period.getEndDate()).isNull();
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("시작일이 종료일보다 빠르면 생성한다")
        void success_Of_StartBeforeEnd() {
            // given
            LocalDate startDate = LocalDate.of(2026, 10, 16);
            LocalDate endDate = LocalDate.of(2026, 10, 18);

            // when
            FestivalPeriod period = FestivalPeriod.of(startDate, endDate);

            // then
            assertThat(period.getStartDate()).isEqualTo(startDate);
            assertThat(period.getEndDate()).isEqualTo(endDate);
        }

        @Test
        @DisplayName("당일 축제는 날짜 경계값으로 생성한다")
        void success_Of_SameDateBoundary() {
            // given
            LocalDate date = LocalDate.of(2026, 10, 16);

            // when
            FestivalPeriod period = FestivalPeriod.of(date, date);

            // then
            assertThat(period.getStartDate()).isEqualTo(date);
            assertThat(period.getEndDate()).isEqualTo(date);
        }

        @Test
        @DisplayName("시작일이 null이면 생성할 수 없다")
        void fail_Of_NullStartDate_CustomException() {
            // given
            LocalDate startDate = null;
            LocalDate endDate = LocalDate.of(2026, 10, 18);

            // when & then
            assertThatThrownBy(() -> FestivalPeriod.of(startDate, endDate))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("종료일이 null이면 생성할 수 없다")
        void fail_Of_NullEndDate_CustomException() {
            // given
            LocalDate startDate = LocalDate.of(2026, 10, 16);
            LocalDate endDate = null;

            // when & then
            assertThatThrownBy(() -> FestivalPeriod.of(startDate, endDate))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("시작일이 종료일보다 늦으면 생성할 수 없다")
        void fail_Of_StartAfterEnd_CustomException() {
            // given
            LocalDate startDate = LocalDate.of(2026, 10, 18);
            LocalDate endDate = LocalDate.of(2026, 10, 16);

            // when & then
            assertThatThrownBy(() -> FestivalPeriod.of(startDate, endDate))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
