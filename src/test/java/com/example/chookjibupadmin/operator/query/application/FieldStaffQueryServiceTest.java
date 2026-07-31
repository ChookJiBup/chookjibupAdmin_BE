package com.example.chookjibupadmin.operator.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffStatus;
import com.example.chookjibupadmin.operator.query.application.dto.FieldStaffView;
import com.example.chookjibupadmin.operator.query.repository.FieldStaffQueryRepository;
import java.time.LocalDateTime;
import java.util.List;
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
class FieldStaffQueryServiceTest {

    @InjectMocks
    private FieldStaffQueryService queryService;

    @Mock
    private FieldStaffQueryRepository queryRepository;

    @Nested
    @DisplayName("searchByFestivalId")
    class SearchByFestivalId {

        @Test
        @DisplayName("축제 ID와 검색어로 현장 스태프 목록을 검색한다")
        void success_SearchByFestivalId() {
            // given
            Long festivalId = 1L;
            FieldStaffView view = fieldStaffView();
            given(queryRepository.findAllByFestivalId(festivalId))
                    .willReturn(List.of(view, fieldStaffView("worker01", "이해준")));

            // when
            List<FieldStaffView> result =
                    queryService.searchByFestivalId(festivalId, " STAFF ");

            // then
            assertThat(result).containsExactly(view);
        }

        @Test
        @DisplayName("빈 검색어는 전체 조회로 처리한다")
        void success_SearchByFestivalId_BlankKeywordBoundary() {
            // given
            Long festivalId = 1L;
            given(queryRepository.findAllByFestivalId(festivalId))
                    .willReturn(List.of());

            // when
            List<FieldStaffView> result =
                    queryService.searchByFestivalId(festivalId, " ");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("한글 입력 중간 상태로 이름을 검색한다")
        void success_SearchByFestivalId_HangulComposingKeyword() {
            // given
            Long festivalId = 1L;
            FieldStaffView leeHaeJun = fieldStaffView("staff01", "이해준");
            FieldStaffView kimStaff = fieldStaffView("staff02", "김스태프");
            given(queryRepository.findAllByFestivalId(festivalId))
                    .willReturn(List.of(leeHaeJun, kimStaff));

            // when
            List<FieldStaffView> result =
                    queryService.searchByFestivalId(festivalId, "잏");

            // then
            assertThat(result).containsExactly(leeHaeJun);
        }
    }

    @Nested
    @DisplayName("getByFestivalIdAndPublicId")
    class GetByFestivalIdAndPublicId {

        @Test
        @DisplayName("축제 ID와 현장 스태프 UUID로 계정을 조회한다")
        void success_GetByFestivalIdAndPublicId() {
            // given
            Long festivalId = 1L;
            UUID staffId = UUID.randomUUID();
            FieldStaffView view = fieldStaffView(staffId);
            given(queryRepository.findByFestivalIdAndPublicId(
                    festivalId,
                    staffId
            ))
                    .willReturn(Optional.of(view));

            // when
            FieldStaffView result = queryService.getByFestivalIdAndPublicId(
                    festivalId,
                    staffId
            );

            // then
            assertThat(result).isEqualTo(view);
        }

        @Test
        @DisplayName("현장 스태프가 없으면 조회 실패 예외를 던진다")
        void fail_GetByFestivalIdAndPublicId_CustomException() {
            // given
            Long festivalId = 1L;
            UUID staffId = UUID.randomUUID();
            given(queryRepository.findByFestivalIdAndPublicId(
                    festivalId,
                    staffId
            ))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    queryService.getByFestivalIdAndPublicId(festivalId, staffId)
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FIELD_STAFF_NOT_FOUND.getMessage());
        }
    }

    private FieldStaffView fieldStaffView() {
        return fieldStaffView(UUID.randomUUID());
    }

    private FieldStaffView fieldStaffView(UUID staffId) {
        return new FieldStaffView(
                staffId,
                "staff01",
                "김스태프",
                "010-1234-5678",
                LocalDateTime.of(2026, 10, 9, 0, 0),
                LocalDateTime.of(2026, 10, 18, 23, 59),
                FieldStaffStatus.ACTIVE
        );
    }

    private FieldStaffView fieldStaffView(String loginId, String name) {
        return new FieldStaffView(
                UUID.randomUUID(),
                loginId,
                name,
                "010-1234-5678",
                LocalDateTime.of(2026, 10, 9, 0, 0),
                LocalDateTime.of(2026, 10, 18, 23, 59),
                FieldStaffStatus.ACTIVE
        );
    }
}
