package com.example.demoadmin.booth.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.demoadmin.booth.command.domain.BoothOperatingStatus;
import com.example.demoadmin.booth.query.application.dto.BoothView;
import com.example.demoadmin.booth.query.repository.BoothQueryRepository;
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
class BoothQueryServiceTest {

    @InjectMocks
    private BoothQueryService boothQueryService;

    @Mock
    private BoothQueryRepository boothQueryRepository;

    @Nested
    @DisplayName("getByFestivalIdAndPublicId")
    class GetByFestivalIdAndPublicId {

        @Test
        @DisplayName("축제 부스 조회 projection을 반환한다")
        void success_GetByFestivalIdAndPublicId() {
            // given
            BoothView view = view();
            given(boothQueryRepository.findByFestivalIdAndPublicId(
                    1L,
                    view.boothId()
            )).willReturn(Optional.of(view));

            // when
            BoothView result = boothQueryService.getByFestivalIdAndPublicId(
                    1L,
                    view.boothId()
            );

            // then
            assertThat(result).isEqualTo(view);
        }

        @Test
        @DisplayName("축제 부스가 없으면 예외를 던진다")
        void fail_GetByFestivalIdAndPublicId_CustomException() {
            // given
            UUID boothId = UUID.randomUUID();
            given(boothQueryRepository.findByFestivalIdAndPublicId(1L, boothId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> boothQueryService
                    .getByFestivalIdAndPublicId(1L, boothId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.BOOTH_NOT_FOUND.getMessage());
        }
    }

    private BoothView view() {
        return new BoothView(
                UUID.randomUUID(),
                "푸드 부스",
                "먹거리",
                "A-1",
                "설명",
                BoothOperatingStatus.PREPARING,
                null,
                null,
                null,
                0
        );
    }
}
