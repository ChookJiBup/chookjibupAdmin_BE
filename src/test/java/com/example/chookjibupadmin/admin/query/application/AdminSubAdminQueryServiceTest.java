package com.example.chookjibupadmin.admin.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminView;
import com.example.chookjibupadmin.admin.query.repository.AdminSubAdminQueryRepository;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
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
class AdminSubAdminQueryServiceTest {

    @InjectMocks
    private AdminSubAdminQueryService queryService;

    @Mock
    private AdminSubAdminQueryRepository queryRepository;

    @Nested
    @DisplayName("searchInvitedSubAdmins")
    class SearchInvitedSubAdmins {

        @Test
        @DisplayName("축제 ID와 초대한 관리자 ID로 서브관리자 목록을 검색한다")
        void success_SearchInvitedSubAdmins() {
            // given
            Long festivalId = 1L;
            Long invitedByAdminId = 2L;
            AdminSubAdminView view = subAdminView();
            given(queryRepository.searchInvitedSubAdmins(
                    festivalId,
                    invitedByAdminId,
                    "sub"
            ))
                    .willReturn(List.of(view));

            // when
            List<AdminSubAdminView> result =
                    queryService.searchInvitedSubAdmins(
                            festivalId,
                            invitedByAdminId,
                            " sub "
                    );

            // then
            assertThat(result).containsExactly(view);
        }

        @Test
        @DisplayName("서브관리자가 없으면 빈 목록을 반환한다")
        void success_SearchInvitedSubAdmins_EmptyBoundary() {
            // given
            Long festivalId = 1L;
            Long invitedByAdminId = 2L;
            given(queryRepository.searchInvitedSubAdmins(
                    festivalId,
                    invitedByAdminId,
                    null
            ))
                    .willReturn(List.of());

            // when
            List<AdminSubAdminView> result =
                    queryService.searchInvitedSubAdmins(
                            festivalId,
                            invitedByAdminId,
                            " "
                    );

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getInvitedSubAdmin")
    class GetInvitedSubAdmin {

        @Test
        @DisplayName("축제 ID, 초대한 관리자 ID, 관리자 UUID로 서브관리자를 조회한다")
        void success_GetInvitedSubAdmin() {
            // given
            Long festivalId = 1L;
            Long invitedByAdminId = 2L;
            UUID adminId = UUID.randomUUID();
            AdminSubAdminView view = subAdminView(adminId);
            given(queryRepository.findInvitedSubAdmin(
                    festivalId,
                    invitedByAdminId,
                    adminId
            ))
                    .willReturn(Optional.of(view));

            // when
            AdminSubAdminView result =
                    queryService.getInvitedSubAdmin(
                            festivalId,
                            invitedByAdminId,
                            adminId
                    );

            // then
            assertThat(result).isEqualTo(view);
        }

        @Test
        @DisplayName("서브관리자가 없으면 조회 실패 예외를 던진다")
        void fail_GetInvitedSubAdmin_CustomException() {
            // given
            Long festivalId = 1L;
            Long invitedByAdminId = 2L;
            UUID adminId = UUID.randomUUID();
            given(queryRepository.findInvitedSubAdmin(
                    festivalId,
                    invitedByAdminId,
                    adminId
            ))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    queryService.getInvitedSubAdmin(
                            festivalId,
                            invitedByAdminId,
                            adminId
                    )
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.ADMIN_SUB_ADMIN_NOT_FOUND.getMessage());
        }
    }

    private AdminSubAdminView subAdminView() {
        return subAdminView(UUID.randomUUID());
    }

    private AdminSubAdminView subAdminView(UUID adminId) {
        return new AdminSubAdminView(
                adminId,
                "sub@mapo.go.kr",
                "김관리",
                "마포구청 소속",
                "관광정책과",
                "주무관",
                AdminStatus.ACTIVE
        );
    }
}
