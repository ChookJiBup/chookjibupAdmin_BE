package com.example.chookjibupadmin.admin.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalCondition;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalView;
import com.example.chookjibupadmin.admin.query.repository.AdminManagedFestivalQueryRepository;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalDate;
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
class AdminManagedFestivalQueryServiceTest {

    @InjectMocks
    private AdminManagedFestivalQueryService queryService;

    @Mock
    private AdminManagedFestivalQueryRepository queryRepository;

    @Nested
    @DisplayName("searchCurrentManagedFestivals")
    class SearchCurrentManagedFestivals {

        @Test
        @DisplayName("관리자 ID와 조건으로 현재 관리 축제 목록을 조회한다")
        void success_SearchCurrentManagedFestivals() {
            // given
            Long adminAccountId = 1L;
            AdminManagedFestivalCondition condition =
                    new AdminManagedFestivalCondition(
                            AdminRole.FESTIVAL_OWNER,
                            2026,
                            " MAPO "
                    );
            AdminManagedFestivalCondition normalized =
                    new AdminManagedFestivalCondition(
                            AdminRole.FESTIVAL_OWNER,
                            2026,
                            "mapo"
                    );
            AdminManagedFestivalView view = managedFestivalView();
            given(queryRepository.searchCurrentManagedFestivals(
                    adminAccountId,
                    normalized
            )).willReturn(List.of(view));

            // when
            List<AdminManagedFestivalView> result =
                    queryService.searchCurrentManagedFestivals(
                            adminAccountId,
                            condition
                    );

            // then
            assertThat(result).containsExactly(view);
        }
    }

    @Nested
    @DisplayName("getCurrentManagedFestival")
    class GetCurrentManagedFestival {

        @Test
        @DisplayName("관리자 ID와 축제 UUID로 현재 관리 축제를 조회한다")
        void success_GetCurrentManagedFestival() {
            // given
            Long adminAccountId = 1L;
            UUID festivalId = UUID.randomUUID();
            AdminManagedFestivalView view = managedFestivalView(festivalId);
            given(queryRepository.findCurrentManagedFestival(
                    adminAccountId,
                    festivalId
            )).willReturn(Optional.of(view));

            // when
            AdminManagedFestivalView result =
                    queryService.getCurrentManagedFestival(
                            adminAccountId,
                            festivalId
                    );

            // then
            assertThat(result).isEqualTo(view);
        }

        @Test
        @DisplayName("관리 축제가 없으면 축제 없음 예외를 던진다")
        void fail_GetCurrentManagedFestival_CustomException() {
            // given
            Long adminAccountId = 1L;
            UUID festivalId = UUID.randomUUID();
            given(queryRepository.findCurrentManagedFestival(
                    adminAccountId,
                    festivalId
            )).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    queryService.getCurrentManagedFestival(
                            adminAccountId,
                            festivalId
                    )
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_NOT_FOUND.getMessage());
        }
    }

    private AdminManagedFestivalView managedFestivalView() {
        return managedFestivalView(UUID.randomUUID());
    }

    private AdminManagedFestivalView managedFestivalView(UUID festivalId) {
        return new AdminManagedFestivalView(
                festivalId,
                "마포나루 새우젓축제",
                2026,
                AdminRole.FESTIVAL_OWNER,
                FestivalStatus.DRAFT,
                "서울특별시 마포구 월드컵로 243",
                "월드컵공원",
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18)
        );
    }
}
