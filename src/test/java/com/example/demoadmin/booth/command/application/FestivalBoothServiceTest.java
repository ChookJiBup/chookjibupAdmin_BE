package com.example.demoadmin.booth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.demoadmin.booth.command.domain.FestivalBooth;
import com.example.demoadmin.booth.command.domain.FestivalBoothRepository;
import com.example.demoadmin.booth.command.domain.vo.BoothCategory;
import com.example.demoadmin.booth.command.domain.vo.BoothDescription;
import com.example.demoadmin.booth.command.domain.vo.BoothLocation;
import com.example.demoadmin.booth.command.domain.vo.BoothName;
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
class FestivalBoothServiceTest {

    @InjectMocks
    private FestivalBoothService festivalBoothService;

    @Mock
    private FestivalBoothRepository festivalBoothRepository;

    @Nested
    @DisplayName("getByFestivalIdAndPublicIdForUpdate")
    class GetByFestivalIdAndPublicIdForUpdate {

        @Test
        @DisplayName("수정 잠금으로 축제 부스를 조회한다")
        void success_GetByFestivalIdAndPublicIdForUpdate() {
            // given
            FestivalBooth booth = booth();
            given(festivalBoothRepository.findByFestivalIdAndPublicIdForUpdate(
                    booth.getFestivalId(),
                    booth.getPublicId()
            )).willReturn(Optional.of(booth));

            // when
            FestivalBooth result = festivalBoothService
                    .getByFestivalIdAndPublicIdForUpdate(
                            booth.getFestivalId(),
                            booth.getPublicId()
                    );

            // then
            assertThat(result).isEqualTo(booth);
        }

        @Test
        @DisplayName("축제 부스가 없으면 예외를 던진다")
        void fail_GetByFestivalIdAndPublicIdForUpdate_CustomException() {
            // given
            UUID boothId = UUID.randomUUID();
            given(festivalBoothRepository.findByFestivalIdAndPublicIdForUpdate(
                    1L,
                    boothId
            )).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> festivalBoothService
                    .getByFestivalIdAndPublicIdForUpdate(1L, boothId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.BOOTH_NOT_FOUND.getMessage());
        }
    }

    private FestivalBooth booth() {
        return FestivalBooth.create(
                1L,
                BoothName.of("푸드 부스"),
                BoothCategory.of("먹거리"),
                BoothLocation.of("A-1"),
                BoothDescription.of("설명")
        );
    }
}
