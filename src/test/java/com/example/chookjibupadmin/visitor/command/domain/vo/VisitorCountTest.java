package com.example.chookjibupadmin.visitor.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class VisitorCountTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("0 이상 방문 인원 수를 생성한다")
        void success_Of_Zero() {
            // given

            // when
            VisitorCount visitorCount = VisitorCount.of(0);

            // then
            assertThat(visitorCount.getValue()).isZero();
        }

        @Test
        @DisplayName("양수 방문 인원 수를 생성한다")
        void success_Of_Positive() {
            // given

            // when
            VisitorCount visitorCount = VisitorCount.of(12000);

            // then
            assertThat(visitorCount.getValue()).isEqualTo(12000);
        }

        @Test
        @DisplayName("음수 방문 인원 수는 생성할 수 없다")
        void fail_Of_Negative_CustomException() {
            // given

            // when & then
            assertThatThrownBy(() -> VisitorCount.of(-1))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
