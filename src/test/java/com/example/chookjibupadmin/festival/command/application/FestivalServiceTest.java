package com.example.chookjibupadmin.festival.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalRepository;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalDate;
import java.time.LocalTime;
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
class FestivalServiceTest {

    @InjectMocks
    private FestivalService festivalService;

    @Mock
    private FestivalRepository festivalRepository;

    @Nested
    @DisplayName("getByPublicId")
    class GetByPublicId {

        @Test
        @DisplayName("외부 UUID로 축제를 조회한다")
        void success_GetByPublicId() {
            // given
            Festival festival = festival();
            given(festivalRepository.findByPublicId(festival.getPublicId()))
                    .willReturn(Optional.of(festival));

            // when
            Festival found = festivalService.getByPublicId(festival.getPublicId());

            // then
            assertThat(found).isSameAs(festival);
        }

        @Test
        @DisplayName("축제가 없으면 예외를 던진다")
        void fail_GetByPublicId_CustomException() {
            // given
            UUID publicId = UUID.randomUUID();
            given(festivalRepository.findByPublicId(publicId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> festivalService.getByPublicId(publicId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("getByPublicIdForUpdate")
    class GetByPublicIdForUpdate {

        @Test
        @DisplayName("외부 UUID로 축제를 비관적 잠금 조회한다")
        void success_GetByPublicIdForUpdate() {
            Festival festival = festival();
            given(festivalRepository.findByPublicIdForUpdate(festival.getPublicId()))
                    .willReturn(Optional.of(festival));

            Festival found = festivalService.getByPublicIdForUpdate(festival.getPublicId());

            assertThat(found).isSameAs(festival);
        }

        @Test
        @DisplayName("잠금 조회할 축제가 없으면 예외를 던진다")
        void fail_GetByPublicIdForUpdate_CustomException() {
            UUID publicId = UUID.randomUUID();
            given(festivalRepository.findByPublicIdForUpdate(publicId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> festivalService.getByPublicIdForUpdate(publicId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("getByIdForUpdate")
    class GetByIdForUpdate {

        @Test
        @DisplayName("축제 교체 작업용 비관적 잠금 조회를 수행한다")
        void success_GetByIdForUpdate() {
            Festival festival = festival();
            given(festivalRepository.findByIdForUpdate(1L))
                    .willReturn(Optional.of(festival));

            Festival found = festivalService.getByIdForUpdate(1L);

            assertThat(found).isSameAs(festival);
        }

        @Test
        @DisplayName("잠금 조회할 축제가 없으면 예외를 던진다")
        void fail_GetByIdForUpdate_CustomException() {
            given(festivalRepository.findByIdForUpdate(1L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> festivalService.getByIdForUpdate(1L))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_NOT_FOUND.getMessage());
        }
    }

    private Festival festival() {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("마포구 대표 지역 축제"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 18)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }
}
