package com.example.chookjibupadmin.festival.query.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InternalFestivalProgressStatusTest {

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("기준일이 시작일 전이면 진행 예정이다")
        void success_From_Upcoming() {
            // given
            LocalDate today = LocalDate.of(2026, 10, 8);

            // when
            InternalFestivalProgressStatus result =
                    InternalFestivalProgressStatus.from(
                            today,
                            LocalDate.of(2026, 10, 9),
                            LocalDate.of(2026, 10, 18)
                    );

            // then
            assertThat(result).isEqualTo(InternalFestivalProgressStatus.UPCOMING);
        }

        @Test
        @DisplayName("기준일이 시작일이면 진행 중이다")
        void success_From_OngoingStartBoundary() {
            // given
            LocalDate today = LocalDate.of(2026, 10, 9);

            // when
            InternalFestivalProgressStatus result =
                    InternalFestivalProgressStatus.from(
                            today,
                            LocalDate.of(2026, 10, 9),
                            LocalDate.of(2026, 10, 18)
                    );

            // then
            assertThat(result).isEqualTo(InternalFestivalProgressStatus.ONGOING);
        }

        @Test
        @DisplayName("기준일이 종료일이면 진행 중이다")
        void success_From_OngoingEndBoundary() {
            // given
            LocalDate today = LocalDate.of(2026, 10, 18);

            // when
            InternalFestivalProgressStatus result =
                    InternalFestivalProgressStatus.from(
                            today,
                            LocalDate.of(2026, 10, 9),
                            LocalDate.of(2026, 10, 18)
                    );

            // then
            assertThat(result).isEqualTo(InternalFestivalProgressStatus.ONGOING);
        }

        @Test
        @DisplayName("기준일이 종료일 후면 진행 완료이다")
        void success_From_Completed() {
            // given
            LocalDate today = LocalDate.of(2026, 10, 19);

            // when
            InternalFestivalProgressStatus result =
                    InternalFestivalProgressStatus.from(
                            today,
                            LocalDate.of(2026, 10, 9),
                            LocalDate.of(2026, 10, 18)
                    );

            // then
            assertThat(result).isEqualTo(InternalFestivalProgressStatus.COMPLETED);
        }
    }
}
