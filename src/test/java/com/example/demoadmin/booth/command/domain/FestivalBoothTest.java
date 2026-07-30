package com.example.demoadmin.booth.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.booth.command.domain.vo.BoothCategory;
import com.example.demoadmin.booth.command.domain.vo.BoothDescription;
import com.example.demoadmin.booth.command.domain.vo.BoothLineLabel;
import com.example.demoadmin.booth.command.domain.vo.BoothLocation;
import com.example.demoadmin.booth.command.domain.vo.BoothName;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FestivalBoothTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("축제 부스를 생성한다")
        void success_Create() {
            // given
            Long festivalId = 1L;

            // when
            FestivalBooth booth = booth(festivalId);

            // then
            assertThat(booth.getPublicId()).isNotNull();
            assertThat(booth.getFestivalId()).isEqualTo(festivalId);
            assertThat(booth.getOperatingStatus()).isEqualTo(BoothOperatingStatus.PREPARING);
            assertThat(booth.getExpectedWaitingMinutes()).isZero();
        }

        @Test
        @DisplayName("축제 ID가 없으면 생성할 수 없다")
        void fail_Create_CustomException() {
            // given
            Long festivalId = null;

            // when & then
            assertThatThrownBy(() -> booth(festivalId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    @Nested
    @DisplayName("updateBasicInfo")
    class UpdateBasicInfo {

        @Test
        @DisplayName("부스 기본 정보를 수정한다")
        void success_UpdateBasicInfo() {
            // given
            FestivalBooth booth = booth(1L);

            // when
            booth.updateBasicInfo(
                    BoothName.of("수정 부스"),
                    BoothCategory.of("체험"),
                    BoothLocation.of("B-1"),
                    BoothDescription.of("수정 설명")
            );

            // then
            assertThat(booth.getNameValue()).isEqualTo("수정 부스");
            assertThat(booth.getCategoryValue()).isEqualTo("체험");
            assertThat(booth.getLocationValue()).isEqualTo("B-1");
            assertThat(booth.getDescriptionValue()).isEqualTo("수정 설명");
        }
    }

    @Nested
    @DisplayName("updateQueueTail")
    class UpdateQueueTail {

        @Test
        @DisplayName("현재 줄 끝을 대기 라인으로 갱신한다")
        void success_UpdateQueueTail() {
            // given
            FestivalBooth booth = savedBooth(10L, 1L);
            BoothQueueLine queueLine = savedQueueLine(20L, booth.getId(), 3, 15);

            // when
            booth.updateQueueTail(queueLine);

            // then
            assertThat(booth.getCurrentQueueLineId()).isEqualTo(queueLine.getId());
            assertThat(booth.getExpectedWaitingMinutes()).isEqualTo(15);
            assertThat(booth.getOperatingStatus()).isEqualTo(BoothOperatingStatus.OPERATING);
        }

        @Test
        @DisplayName("다른 부스 대기 라인으로 줄 끝을 갱신할 수 없다")
        void fail_UpdateQueueTail_CustomException() {
            // given
            FestivalBooth booth = savedBooth(10L, 1L);
            BoothQueueLine queueLine = savedQueueLine(20L, 2L, 1, 10);

            // when & then
            assertThatThrownBy(() -> booth.updateQueueTail(queueLine))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.BOOTH_QUEUE_LINE_NOT_BELONG_TO_BOOTH.getMessage());
        }

        @Test
        @DisplayName("마감된 부스는 줄 끝을 갱신할 수 없다")
        void fail_UpdateQueueTail_CustomException_BoothClosed() {
            // given
            FestivalBooth booth = savedBooth(10L, 1L);
            BoothQueueLine queueLine = savedQueueLine(20L, booth.getId(), 1, 10);
            booth.close();

            // when & then
            assertThatThrownBy(() -> booth.updateQueueTail(queueLine))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.BOOTH_CLOSED.getMessage());
        }
    }

    @Nested
    @DisplayName("saturateWith")
    class SaturateWith {

        @Test
        @DisplayName("현재 줄 끝을 유지하며 혼잡 상태로 변경한다")
        void success_SaturateWith() {
            // given
            FestivalBooth booth = savedBooth(10L, 1L);
            BoothQueueLine queueLine = savedQueueLine(20L, booth.getId(), 1, 30);

            // when
            booth.saturateWith(queueLine);

            // then
            assertThat(booth.getCurrentQueueLineId()).isEqualTo(queueLine.getId());
            assertThat(booth.getOperatingStatus()).isEqualTo(BoothOperatingStatus.SATURATED);
        }

        @Test
        @DisplayName("마감된 부스는 혼잡 상태로 변경할 수 없다")
        void fail_SaturateWith_CustomException() {
            // given
            FestivalBooth booth = savedBooth(10L, 1L);
            BoothQueueLine queueLine = savedQueueLine(20L, booth.getId(), 1, 30);
            booth.close();

            // when & then
            assertThatThrownBy(() -> booth.saturateWith(queueLine))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.BOOTH_CLOSED.getMessage());
        }
    }

    private FestivalBooth booth(Long festivalId) {
        return FestivalBooth.create(
                festivalId,
                BoothName.of("푸드 부스"),
                BoothCategory.of("먹거리"),
                BoothLocation.of("A-1"),
                BoothDescription.of("대표 먹거리 부스")
        );
    }

    private FestivalBooth savedBooth(
            Long id,
            Long festivalId
    ) {
        FestivalBooth booth = booth(festivalId);
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }

    private BoothQueueLine savedQueueLine(
            Long id,
            Long boothId,
            int lineOrder,
            int expectedWaitingMinutes
    ) {
        BoothQueueLine queueLine = BoothQueueLine.create(
                boothId,
                lineOrder,
                BoothLineLabel.of("라인 " + lineOrder),
                expectedWaitingMinutes,
                100,
                "{}",
                "{}"
        );
        ReflectionTestUtils.setField(queueLine, "id", id);
        return queueLine;
    }
}
