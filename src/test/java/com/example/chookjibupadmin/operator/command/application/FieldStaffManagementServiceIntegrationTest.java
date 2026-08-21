package com.example.chookjibupadmin.operator.command.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.operator.command.application.dto.CreateFieldStaffCommand;
import com.example.chookjibupadmin.operator.command.application.dto.CreateFieldStaffResult;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FieldStaffManagementServiceIntegrationTest {

    @Autowired
    private FieldStaffManagementService managementService;

    @Autowired
    private FieldStaffAccountService fieldStaffAccountService;

    @Autowired
    private FestivalService festivalService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("현장 스태프 계정을 DB에 저장한다")
        void success_Create_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount adminAccount = persistOwner(festival);
            CreateFieldStaffCommand command = createCommand();

            // when
            CreateFieldStaffResult result = managementService.create(
                    festival.getPublicId(),
                    command,
                    principal(adminAccount)
            );

            // then
            FieldStaffAccount found = fieldStaffAccountService.getById(
                    result.fieldStaffAccount().getId()
            );
            assertThat(found.getLoginIdValue()).isEqualTo("staff01");
            assertThat(found.getPasswordHashValue()).isNotEqualTo(result.temporaryPassword());
            assertThat(found.getValidFrom()).isEqualTo(festival.getStartDate().minusDays(7).atStartOfDay());
            assertThat(found.getValidUntil()).isEqualTo(festival.getEndDate().atTime(LocalTime.MAX));
        }

        @Test
        @DisplayName("외부업자 운영자도 현장 스태프를 생성할 수 있다")
        void success_Create_ContractorSubAdmin() {
            Festival festival = festivalService.save(festival());
            AdminAccount owner = persistOwner(festival);
            AdminAccount operator = adminAccountService.save(AdminAccount.createContractor(
                    AdminEmail.of("vendor@gmail.com"),
                    AdminName.of("김업체"),
                    AdminOrganization.of("축제기획(주)"),
                    AdminPasswordHash.of("encoded-password")
            ));
            adminFestivalRoleService.assignSubAdmin(
                    operator.getId(),
                    festival.getId(),
                    owner.getId()
            );

            CreateFieldStaffResult result = managementService.create(
                    festival.getPublicId(),
                    createCommand("staff-op"),
                    principal(operator)
            );

            FieldStaffAccount found = fieldStaffAccountService.getById(
                    result.fieldStaffAccount().getId()
            );
            assertThat(found.getLoginIdValue()).isEqualTo("staff-op");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("현장 스태프 계정을 삭제 상태로 저장한다")
        void success_Delete_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount adminAccount = persistOwner(festival);
            CreateFieldStaffResult result = managementService.create(
                    festival.getPublicId(),
                    createCommand(),
                    principal(adminAccount)
            );

            // when
            managementService.delete(
                    festival.getPublicId(),
                    result.fieldStaffAccount().getPublicId(),
                    principal(adminAccount)
            );

            // then
            FieldStaffAccount found = fieldStaffAccountService.getById(
                    result.fieldStaffAccount().getId()
            );
            assertThat(found.getStatus()).isEqualTo(FieldStaffStatus.DELETED);
        }
    }

    @Nested
    @DisplayName("deleteAll")
    class DeleteAll {

        @Test
        @DisplayName("여러 현장 스태프 계정을 한 트랜잭션에서 삭제 상태로 저장한다")
        void success_DeleteAll_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount adminAccount = persistOwner(festival);
            CreateFieldStaffResult first = managementService.create(
                    festival.getPublicId(),
                    createCommand("staff01"),
                    principal(adminAccount)
            );
            CreateFieldStaffResult second = managementService.create(
                    festival.getPublicId(),
                    createCommand("staff02"),
                    principal(adminAccount)
            );

            // when
            managementService.deleteAll(
                    festival.getPublicId(),
                    java.util.List.of(
                            first.fieldStaffAccount().getPublicId(),
                            second.fieldStaffAccount().getPublicId()
                    ),
                    principal(adminAccount)
            );

            // then
            assertThat(fieldStaffAccountService.getById(
                    first.fieldStaffAccount().getId()).getStatus())
                    .isEqualTo(FieldStaffStatus.DELETED);
            assertThat(fieldStaffAccountService.getById(
                    second.fieldStaffAccount().getId()).getStatus())
                    .isEqualTo(FieldStaffStatus.DELETED);
        }
    }

    private CreateFieldStaffCommand createCommand() {
        return createCommand("staff01");
    }

    private CreateFieldStaffCommand createCommand(String loginId) {
        return new CreateFieldStaffCommand(
                loginId,
                "김스태프",
                "010-1234-5678"
        );
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(
                adminAccount.getId(),
                adminAccount.getEmailValue()
        );
    }

    private AdminAccount persistOwner(Festival festival) {
        AdminAccount owner = adminAccountService.save(AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        ));
        adminFestivalRoleService.assignFestivalOwner(owner.getId(), festival.getId());
        return owner;
    }

    private Festival festival() {
        LocalDate today = LocalDate.now();
        return Festival.create(
                10L,
                java.util.UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("마포구 대표 지역 축제"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(today.plusDays(1), today.plusDays(2)),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }
}
