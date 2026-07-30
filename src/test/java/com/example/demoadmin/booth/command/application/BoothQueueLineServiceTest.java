package com.example.demoadmin.booth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.demoadmin.booth.command.domain.BoothQueueLine;
import com.example.demoadmin.booth.command.domain.BoothQueueLineRepository;
import com.example.demoadmin.booth.command.domain.vo.BoothLineLabel;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoothQueueLineServiceTest {

    @InjectMocks
    private BoothQueueLineService boothQueueLineService;

    @Mock
    private BoothQueueLineRepository boothQueueLineRepository;

    @Nested
    @DisplayName("getByBoothIdAndPublicId")
    class GetByBoothIdAndPublicId {

        @Test
        @DisplayName("부스에 속한 대기 라인을 조회한다")
        void success_GetByBoothIdAndPublicId() {
            // given
            BoothQueueLine queueLine = queueLine();
            given(boothQueueLineRepository.findByBoothIdAndPublicId(
                    queueLine.getBoothId(),
                    queueLine.getPublicId()
            )).willReturn(Optional.of(queueLine));

            // when
            BoothQueueLine result = boothQueueLineService.getByBoothIdAndPublicId(
                    queueLine.getBoothId(),
                    queueLine.getPublicId()
            );

            // then
            assertThat(result).isEqualTo(queueLine);
        }

        @Test
        @DisplayName("대기 라인이 없으면 예외를 던진다")
        void fail_GetByBoothIdAndPublicId_CustomException() {
            // given
            UUID lineId = UUID.randomUUID();
            given(boothQueueLineRepository.findByBoothIdAndPublicId(1L, lineId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> boothQueueLineService
                    .getByBoothIdAndPublicId(1L, lineId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.BOOTH_QUEUE_LINE_NOT_FOUND.getMessage());
        }
    }

    private BoothQueueLine queueLine() {
        return BoothQueueLine.create(
                1L,
                1,
                BoothLineLabel.of("라인 1"),
                10,
                30,
                "{}",
                "{}"
        );
    }
}
