package com.example.chookjibupadmin.admin.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminCandidateView;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminSubAdminCandidateQueryApplicationServiceTest {

    @InjectMocks
    private AdminSubAdminCandidateQueryApplicationService applicationService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private AdminSubAdminCandidateQueryService candidateQueryService;

    @Mock
    private AdminNameEmailSearchMatcher searchMatcher;

    @Nested
    @DisplayName("searchCandidates")
    class SearchCandidates {

        @Test
        @DisplayName("제1 관리자가 초대 가능한 관리자 후보를 검색한다")
        void success_SearchCandidates() {
            // given
            Festival festival = festival(1L);
            AdminAccount owner = owner(festival.getId());
            AdminPrincipal principal = principal(owner);
            AdminSubAdminCandidateView view = candidateView();
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(owner);
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    owner.getId(),
                    festival.getId()
            ))
                    .willReturn(AdminFestivalRole.createFestivalOwner(
                            owner.getId(),
                            festival.getId()
                    ));
            given(candidateQueryService.findCandidates(festival.getId()))
                    .willReturn(List.of(view));
            given(searchMatcher.search(List.of(view), "마포"))
                    .willReturn(List.of(view));

            // when
            List<AdminSubAdminCandidateView> result =
                    applicationService.searchCandidates(
                            festival.getPublicId(),
                            "마포",
                            principal
                    );

            // then
            assertThat(result).containsExactly(view);
        }

        @Test
        @DisplayName("인증 주체가 없으면 인증 예외를 던진다")
        void fail_SearchCandidates_CustomException() {
            // given
            UUID festivalId = UUID.randomUUID();
            AdminPrincipal principal = null;

            // when & then
            assertThatThrownBy(() ->
                    applicationService.searchCandidates(festivalId, null, principal)
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
        }

        @Test
        @DisplayName("제2 관리자는 초대 후보를 조회할 수 없다")
        void fail_SearchCandidates_SubAdmin_CustomException() {
            // given
            Festival festival = festival(1L);
            AdminAccount subAdmin = subAdmin(festival.getId());
            AdminPrincipal principal = principal(subAdmin);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(subAdmin);
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    subAdmin.getId(),
                    festival.getId()
            ))
                    .willReturn(AdminFestivalRole.createSubAdmin(
                            subAdmin.getId(),
                            festival.getId(),
                            1L
                    ));

            // when & then
            assertThatThrownBy(() ->
                    applicationService.searchCandidates(
                            festival.getPublicId(),
                            null,
                            principal
                    )
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FORBIDDEN.getMessage());
        }
    }

    private AdminAccount owner(Long festivalId) {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(adminAccount, "id", 1L);
        return adminAccount;
    }

    private AdminAccount subAdmin(Long festivalId) {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of("sub@mapo.go.kr"),
                AdminName.of("김관리"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(adminAccount, "id", 2L);
        return adminAccount;
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(
                adminAccount.getId(),
                adminAccount.getEmailValue()
        );
    }

    private Festival festival(Long festivalId) {
        Festival festival = Festival.create(
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
        ReflectionTestUtils.setField(festival, "id", festivalId);
        return festival;
    }

    private AdminSubAdminCandidateView candidateView() {
        return new AdminSubAdminCandidateView(
                UUID.randomUUID(),
                "candidate@mapo.go.kr",
                "김후보",
                "마포구청",
                                "주무관",
                AdminStatus.ACTIVE
        );
    }
}
