package com.example.chookjibupadmin.admin.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.admin.query.application.AdminSubAdminQueryApplicationService;
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
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminSubAdminAssignServiceIntegrationTest {

    @Autowired
    private AdminSubAdminAssignService assignService;

    @Autowired
    private AdminSubAdminQueryApplicationService queryApplicationService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Autowired
    private FestivalService festivalService;

    @Nested
    @DisplayName("assign")
    class Assign {

        @Test
        @DisplayName("제1관리자가 활성 외부업자를 제2관리자로 배정한다")
        void success_Assign() {
            // given
            Festival festival = festivalService.save(festival(1L, "축제 A"));
            AdminAccount owner = adminAccountService.save(government("owner@mapo.go.kr"));
            AdminAccount target = adminAccountService.save(contractor("sub1@partner.com"));
            adminFestivalRoleService.assignFestivalOwner(owner.getId(), festival.getId());

            // when
            AdminFestivalRole role = assignService.assign(
                    festival.getPublicId(),
                    target.getPublicId(),
                    principal(owner)
            );

            // then
            assertThat(role.getAdminAccountId()).isEqualTo(target.getId());
            assertThat(queryApplicationService.getSubAdmins(
                    festival.getPublicId(),
                    null,
                    principal(owner)
            )).hasSize(1);
        }

        /**
         * 첫 요청이 서버에 반영된 뒤 사용자가 실패로 알고 다시 누르는 상황을 재현한다.
         * 재시도가 예외로 끝나면 "저장은 됐는데 실패로 보이는" 상태가 그대로 남는다.
         */
        @Test
        @DisplayName("이미 배정된 대상을 다시 배정해도 실패하지 않고 중복 행도 생기지 않는다")
        void success_Assign_Retry_Idempotent() {
            // given
            Festival festival = festivalService.save(festival(1L, "축제 A"));
            AdminAccount owner = adminAccountService.save(government("owner@mapo.go.kr"));
            AdminAccount target = adminAccountService.save(contractor("sub1@partner.com"));
            adminFestivalRoleService.assignFestivalOwner(owner.getId(), festival.getId());
            AdminFestivalRole first = assignService.assign(
                    festival.getPublicId(),
                    target.getPublicId(),
                    principal(owner)
            );

            // when
            AdminFestivalRole retried = assignService.assign(
                    festival.getPublicId(),
                    target.getPublicId(),
                    principal(owner)
            );

            // then
            assertThat(retried.getPublicId()).isEqualTo(first.getPublicId());
            assertThat(queryApplicationService.getSubAdmins(
                    festival.getPublicId(),
                    null,
                    principal(owner)
            )).hasSize(1);
        }

        @Test
        @DisplayName("재시도가 예외 없이 끝난다")
        void success_Assign_Retry_DoesNotThrow() {
            // given
            Festival festival = festivalService.save(festival(1L, "축제 A"));
            AdminAccount owner = adminAccountService.save(government("owner@mapo.go.kr"));
            AdminAccount target = adminAccountService.save(contractor("sub1@partner.com"));
            adminFestivalRoleService.assignFestivalOwner(owner.getId(), festival.getId());
            assignService.assign(
                    festival.getPublicId(),
                    target.getPublicId(),
                    principal(owner)
            );

            // when & then
            assertThatCode(() -> assignService.assign(
                    festival.getPublicId(),
                    target.getPublicId(),
                    principal(owner)
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("다른 제1관리자가 이미 배정한 대상이면 충돌로 거절한다")
        void fail_Assign_AssignedByOtherOwner_CustomException() {
            // given
            Festival festival = festivalService.save(festival(1L, "축제 A"));
            AdminAccount owner = adminAccountService.save(government("owner@mapo.go.kr"));
            AdminAccount other = adminAccountService.save(government("other@mapo.go.kr"));
            AdminAccount target = adminAccountService.save(contractor("sub1@partner.com"));
            adminFestivalRoleService.assignFestivalOwner(owner.getId(), festival.getId());
            adminFestivalRoleService.assignSubAdmin(
                    target.getId(),
                    festival.getId(),
                    other.getId()
            );

            // when & then
            assertThatThrownBy(() -> assignService.assign(
                    festival.getPublicId(),
                    target.getPublicId(),
                    principal(owner)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_ALREADY_ASSIGNED.getMessage());
        }
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(adminAccount.getId(), adminAccount.getEmailValue());
    }

    private AdminAccount government(String email) {
        return AdminAccount.createGovernment(
                AdminEmail.of(email),
                AdminName.of("김관리"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
    }

    private AdminAccount contractor(String email) {
        return AdminAccount.createContractor(
                AdminEmail.of(email),
                AdminName.of("박운영"),
                AdminOrganization.of("행사기획사"),
                AdminPasswordHash.of("encoded-password")
        );
    }

    private Festival festival(Long seriesId, String name) {
        return Festival.create(
                seriesId,
                UUID.randomUUID(),
                FestivalName.of(name),
                FestivalDescription.of("마포구 대표 지역 축제"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 18)
                ),
                FestivalOperationTime.of(LocalTime.of(10, 0), LocalTime.of(21, 0))
        );
    }
}
