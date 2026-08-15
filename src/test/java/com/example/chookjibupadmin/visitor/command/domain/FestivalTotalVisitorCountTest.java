package com.example.chookjibupadmin.visitor.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FestivalTotalVisitorCountTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("총 방문 인원 수를 생성한다")
        void success_Create() {
            // given

            // when
            FestivalTotalVisitorCount total = FestivalTotalVisitorCount.create(
                    1L,
                    VisitorCount.of(30000)
            );

            // then
            assertThat(total.getFestivalId()).isEqualTo(1L);
            assertThat(total.getVisitorCountValue()).isEqualTo(30000);
        }

        @Test
        @DisplayName("방문 인원 수가 없으면 생성할 수 없다")
        void fail_Create_NullVisitorCount_CustomException() {
            // given

            // when & then
            assertThatThrownBy(() -> FestivalTotalVisitorCount.create(1L, null))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    @Nested
    @DisplayName("changeVisitorCount")
    class ChangeVisitorCount {

        @Test
        @DisplayName("총 방문 인원 수를 변경한다")
        void success_ChangeVisitorCount() {
            // given
            FestivalTotalVisitorCount total = FestivalTotalVisitorCount.create(
                    1L,
                    VisitorCount.of(1000)
            );

            // when
            total.changeVisitorCount(VisitorCount.of(2000));

            // then
            assertThat(total.getVisitorCountValue()).isEqualTo(2000);
        }
    }
}
