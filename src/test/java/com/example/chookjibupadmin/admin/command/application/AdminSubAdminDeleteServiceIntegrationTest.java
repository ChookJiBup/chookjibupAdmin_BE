package com.example.chookjibupadmin.admin.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminSubAdminDeleteServiceIntegrationTest {

    @Autowired
    private AdminSubAdminDeleteService deleteService;

    @Autowired
    private AdminSubAdminQueryApplicationService queryApplicationService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Autowired
    private FestivalService festivalService;

    @Nested
    @DisplayName("deleteAll")
    class DeleteAll {

        @Test
        @DisplayName("제2관리자 계정은 유지하고 담당 축제 권한만 모두 삭제한다")
        void success_DeleteAll_Persisted() {
            // given
            Festival festival = festivalService.save(festival(1L, "축제 A"));
            AdminAccount owner = adminAccountService.save(account("owner@mapo.go.kr"));
            AdminAccount first = adminAccountService.save(account("sub1@mapo.go.kr"));
            AdminAccount second = adminAccountService.save(account("sub2@mapo.go.kr"));
            adminFestivalRoleService.assignFestivalOwner(owner.getId(), festival.getId());
            adminFestivalRoleService.assignSubAdmin(first.getId(), festival.getId(), owner.getId());
            adminFestivalRoleService.assignSubAdmin(second.getId(), festival.getId(), owner.getId());

            // when
            deleteService.deleteAll(
                    festival.getPublicId(),
                    List.of(first.getPublicId(), second.getPublicId()),
                    principal(owner)
            );

            // then
            assertThat(queryApplicationService.getSubAdmins(
                    festival.getPublicId(),
                    null,
                    principal(owner)
            )).isEmpty();
            assertThat(adminAccountService.getByPublicId(first.getPublicId()))
                    .isSameAs(first);
            assertThat(adminAccountService.getByPublicId(second.getPublicId()))
                    .isSameAs(second);
        }

        @Test
        @DisplayName("다른 축제 제2관리자가 섞이면 기존 권한도 삭제하지 않는다")
        void fail_DeleteAll_DifferentFestival_CustomException() {
            // given
            Festival firstFestival = festivalService.save(festival(1L, "축제 A"));
            Festival secondFestival = festivalService.save(festival(2L, "축제 B"));
            AdminAccount firstOwner = adminAccountService.save(
                    account("owner1@mapo.go.kr")
            );
            AdminAccount secondOwner = adminAccountService.save(
                    account("owner2@mapo.go.kr")
            );
            AdminAccount firstSubAdmin = adminAccountService.save(
                    account("sub1@mapo.go.kr")
            );
            AdminAccount secondSubAdmin = adminAccountService.save(
                    account("sub2@mapo.go.kr")
            );
            adminFestivalRoleService.assignFestivalOwner(
                    firstOwner.getId(),
                    firstFestival.getId()
            );
            adminFestivalRoleService.assignFestivalOwner(
                    secondOwner.getId(),
                    secondFestival.getId()
            );
            adminFestivalRoleService.assignSubAdmin(
                    firstSubAdmin.getId(),
                    firstFestival.getId(),
                    firstOwner.getId()
            );
            adminFestivalRoleService.assignSubAdmin(
                    secondSubAdmin.getId(),
                    secondFestival.getId(),
                    secondOwner.getId()
            );

            // when & then
            assertThatThrownBy(() -> deleteService.deleteAll(
                    firstFestival.getPublicId(),
                    List.of(
                            firstSubAdmin.getPublicId(),
                            secondSubAdmin.getPublicId()
                    ),
                    principal(firstOwner)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.ADMIN_SUB_ADMIN_NOT_FOUND.getMessage());
            assertThat(queryApplicationService.getSubAdmins(
                    firstFestival.getPublicId(),
                    null,
                    principal(firstOwner)
            )).hasSize(1);
        }
    }

    private AdminPrincipal principal(AdminAccount account) {
        return new AdminPrincipal(account.getId(), account.getEmailValue());
    }

    private AdminAccount account(String email) {
        return AdminAccount.createAdmin(
                AdminEmail.of(email),
                AdminName.of("김관리"),
                AdminOrganization.of("마포구청 소속"),
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
