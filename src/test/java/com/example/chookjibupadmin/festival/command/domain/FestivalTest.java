package com.example.chookjibupadmin.festival.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FestivalTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("축제 묶음과 개최 연도를 가진 축제를 생성한다")
        void success_Create() {
            // given
            Long seriesId = 1L;

            // when
            Festival festival = festival();

            // then
            assertThat(festival.getPublicId()).isNotNull();
            assertThat(festival.getSeriesId()).isEqualTo(seriesId);
            assertThat(festival.getYear()).isEqualTo(2026);
            assertThat(festival.getDetailAddressValue())
                    .isEqualTo("월드컵공원");
        }

        @Test
        @DisplayName("축제 묶음 ID가 없으면 생성할 수 없다")
        void fail_Create_CustomException() {
            // given
            Long seriesId = null;

            // when & then
            assertThatThrownBy(() -> Festival.create(
                            seriesId,
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
                    ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    @Nested
    @DisplayName("updateBasicInfo")
    class UpdateBasicInfo {

        @Test
        @DisplayName("축제 기본 정보를 수정한다")
        void success_UpdateBasicInfo() {
            // given
            Festival festival = festival();

            // when
            festival.updateBasicInfo(
                    FestivalName.of("수정 축제"),
                    FestivalDescription.of("수정 설명"),
                    FestivalAddress.of("서울특별시 마포구 수정로 1"),
                    FestivalDetailAddress.of("수정 행사장"),
                    FestivalPeriod.of(
                            LocalDate.of(2026, 11, 1),
                            LocalDate.of(2026, 11, 3)
                    ),
                    FestivalOperationTime.of(
                            LocalTime.of(9, 0),
                            LocalTime.of(20, 0)
                    )
            );

            // then
            assertThat(festival.getNameValue()).isEqualTo("수정 축제");
            assertThat(festival.getDescriptionValue()).isEqualTo("수정 설명");
            assertThat(festival.getDetailAddressValue())
                    .isEqualTo("수정 행사장");
            assertThat(festival.getStartDate())
                    .isEqualTo(LocalDate.of(2026, 11, 1));
        }

        @Test
        @DisplayName("축제 개최 연도가 바뀌는 기본 정보 수정은 할 수 없다")
        void fail_UpdateBasicInfo_YearChanged_CustomException() {
            // given
            Festival festival = festival();

            // when & then
            assertThatThrownBy(() -> festival.updateBasicInfo(
                            FestivalName.of("수정 축제"),
                            FestivalDescription.of("수정 설명"),
                            FestivalAddress.of("서울특별시 마포구 수정로 1"),
                            FestivalPeriod.of(
                                    LocalDate.of(2027, 11, 1),
                                    LocalDate.of(2027, 11, 3)
                            ),
                            FestivalOperationTime.of(
                                    LocalTime.of(9, 0),
                                    LocalTime.of(20, 0)
                            )
                    ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_YEAR_CANNOT_BE_CHANGED.getMessage());
        }

        @Test
        @DisplayName("개최 연도가 없는 기존 축제는 기본 정보를 수정할 수 없다")
        void fail_UpdateBasicInfo_YearMissing_CustomException() {
            // given
            Festival festival = festival();
            ReflectionTestUtils.setField(festival, "year", null);

            // when & then
            assertThatThrownBy(() -> festival.updateBasicInfo(
                            FestivalName.of("수정 축제"),
                            FestivalDescription.of("수정 설명"),
                            FestivalAddress.of("서울특별시 마포구 수정로 1"),
                            FestivalPeriod.of(
                                    LocalDate.of(2026, 11, 1),
                                    LocalDate.of(2026, 11, 3)
                            ),
                            FestivalOperationTime.of(
                                    LocalTime.of(9, 0),
                                    LocalTime.of(20, 0)
                            )
                    ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_YEAR_CANNOT_BE_CHANGED.getMessage());
        }
    }

    private Festival festival() {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("마포구 대표 지역 축제"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalDetailAddress.of("월드컵공원"),
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
