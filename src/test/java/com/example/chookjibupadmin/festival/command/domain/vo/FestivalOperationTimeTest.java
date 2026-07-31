package com.example.chookjibupadmin.festival.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FestivalOperationTimeTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("JPA 기본 생성자로 생성할 수 있다")
        void success_Constructor_ForJpa() {
            // given

            // when
            FestivalOperationTime operationTime = new FestivalOperationTime();

            // then
            assertThat(operationTime.getStartTime()).isNull();
            assertThat(operationTime.getEndTime()).isNull();
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("시작 시간이 종료 시간보다 빠르면 생성한다")
        void success_Of_StartBeforeEnd() {
            // given
            LocalTime startTime = LocalTime.of(10, 0);
            LocalTime endTime = LocalTime.of(21, 0);

            // when
            FestivalOperationTime operationTime = FestivalOperationTime.of(
                    startTime,
                    endTime
            );

            // then
            assertThat(operationTime.getStartTime()).isEqualTo(startTime);
            assertThat(operationTime.getEndTime()).isEqualTo(endTime);
        }

        @Test
        @DisplayName("1분 차이는 운영 시간 최소 간격 경계값으로 생성한다")
        void success_Of_OneMinuteBoundary() {
            // given
            LocalTime startTime = LocalTime.of(10, 0);
            LocalTime endTime = LocalTime.of(10, 1);

            // when
            FestivalOperationTime operationTime = FestivalOperationTime.of(
                    startTime,
                    endTime
            );

            // then
            assertThat(operationTime.getStartTime()).isEqualTo(startTime);
            assertThat(operationTime.getEndTime()).isEqualTo(endTime);
        }

        @Test
        @DisplayName("시작 시간이 null이면 생성할 수 없다")
        void fail_Of_NullStartTime_CustomException() {
            // given
            LocalTime startTime = null;
            LocalTime endTime = LocalTime.of(21, 0);

            // when & then
            assertThatThrownBy(() -> FestivalOperationTime.of(startTime, endTime))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("종료 시간이 null이면 생성할 수 없다")
        void fail_Of_NullEndTime_CustomException() {
            // given
            LocalTime startTime = LocalTime.of(10, 0);
            LocalTime endTime = null;

            // when & then
            assertThatThrownBy(() -> FestivalOperationTime.of(startTime, endTime))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("시작 시간과 종료 시간이 같으면 생성할 수 없다")
        void fail_Of_SameTime_CustomException() {
            // given
            LocalTime startTime = LocalTime.of(10, 0);
            LocalTime endTime = LocalTime.of(10, 0);

            // when & then
            assertThatThrownBy(() -> FestivalOperationTime.of(startTime, endTime))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("시작 시간이 종료 시간보다 늦으면 생성할 수 없다")
        void fail_Of_StartAfterEnd_CustomException() {
            // given
            LocalTime startTime = LocalTime.of(21, 0);
            LocalTime endTime = LocalTime.of(10, 0);

            // when & then
            assertThatThrownBy(() -> FestivalOperationTime.of(startTime, endTime))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
