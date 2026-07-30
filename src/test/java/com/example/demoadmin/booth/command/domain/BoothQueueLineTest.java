package com.example.demoadmin.booth.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.booth.command.domain.vo.BoothLineLabel;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BoothQueueLineTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("부스 대기 라인을 생성한다")
        void success_Create() {
            // given
            Long boothId = 1L;

            // when
            BoothQueueLine queueLine = queueLine(boothId, 1, 10, 100);

            // then
            assertThat(queueLine.getPublicId()).isNotNull();
            assertThat(queueLine.getBoothId()).isEqualTo(boothId);
            assertThat(queueLine.getLineOrder()).isOne();
            assertThat(queueLine.getPathData()).isEqualTo("{}");
            assertThat(queueLine.getEntryPointData()).isEqualTo("{}");
        }

        @Test
        @DisplayName("부스 ID가 없으면 생성할 수 없다")
        void fail_Create_CustomException_BoothIdNull() {
            // given
            Long boothId = null;

            // when & then
            assertThatThrownBy(() -> queueLine(boothId, 1, 10, 100))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("대기 라인 순서는 1 이상이어야 한다")
        void fail_Create_CustomException_LineOrderZero() {
            // given
            int lineOrder = 0;

            // when & then
            assertThatThrownBy(() -> queueLine(1L, lineOrder, 10, 100))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("예상 대기 시간은 0 이상이어야 한다")
        void fail_Create_CustomException_ExpectedWaitingMinutesNegative() {
            // given
            int expectedWaitingMinutes = -1;

            // when & then
            assertThatThrownBy(() -> queueLine(1L, 1, expectedWaitingMinutes, 100))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("부스 대기 라인 정보를 수정한다")
        void success_Update() {
            // given
            BoothQueueLine queueLine = queueLine(1L, 1, 10, 100);

            // when
            queueLine.update(
                    2,
                    BoothLineLabel.of("두 번째 라인"),
                    20,
                    80,
                    "{\"points\":[]}",
                    "{\"x\":1}"
            );

            // then
            assertThat(queueLine.getLineOrder()).isEqualTo(2);
            assertThat(queueLine.getLabelValue()).isEqualTo("두 번째 라인");
            assertThat(queueLine.getExpectedWaitingMinutes()).isEqualTo(20);
            assertThat(queueLine.getMaxCapacity()).isEqualTo(80);
        }

        @Test
        @DisplayName("수정 시 최대 수용 인원은 0 이상이어야 한다")
        void fail_Update_CustomException_MaxCapacityNegative() {
            // given
            BoothQueueLine queueLine = queueLine(1L, 1, 10, 100);

            // when & then
            assertThatThrownBy(() -> queueLine.update(
                    1,
                    BoothLineLabel.of("첫 번째 라인"),
                    10,
                    -1,
                    "{}",
                    "{}"
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    private BoothQueueLine queueLine(
            Long boothId,
            int lineOrder,
            int expectedWaitingMinutes,
            int maxCapacity
    ) {
        return BoothQueueLine.create(
                boothId,
                lineOrder,
                BoothLineLabel.of("첫 번째 라인"),
                expectedWaitingMinutes,
                maxCapacity,
                null,
                " "
        );
    }
}
