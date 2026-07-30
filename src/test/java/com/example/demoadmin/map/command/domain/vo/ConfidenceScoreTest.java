package com.example.demoadmin.map.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConfidenceScoreTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("신뢰도를 생성한다")
        void success_Of() {
            // given
            double value = 0.82;

            // when
            ConfidenceScore score = ConfidenceScore.of(value);

            // then
            assertThat(score.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("신뢰도는 0 경계값을 허용한다")
        void success_Of_MinBoundary() {
            // given
            double value = 0;

            // when
            ConfidenceScore score = ConfidenceScore.of(value);

            // then
            assertThat(score.getValue()).isZero();
        }

        @Test
        @DisplayName("신뢰도는 1 경계값을 허용한다")
        void success_Of_MaxBoundary() {
            // given
            double value = 1;

            // when
            ConfidenceScore score = ConfidenceScore.of(value);

            // then
            assertThat(score.getValue()).isOne();
        }

        @Test
        @DisplayName("신뢰도가 0보다 작으면 생성할 수 없다")
        void fail_Of_CustomException_UnderMinBoundary() {
            // given
            double value = -0.01;

            // when & then
            assertThatThrownBy(() -> ConfidenceScore.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("신뢰도가 1보다 크면 생성할 수 없다")
        void fail_Of_CustomException_OverMaxBoundary() {
            // given
            double value = 1.01;

            // when & then
            assertThatThrownBy(() -> ConfidenceScore.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
