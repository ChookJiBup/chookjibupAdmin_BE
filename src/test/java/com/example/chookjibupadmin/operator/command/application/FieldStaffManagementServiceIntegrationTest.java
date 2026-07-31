package com.example.chookjibupadmin.operator.command.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
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

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("현장 스태프 계정을 DB에 저장한다")
        void success_Create_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount adminAccount = adminAccountService.save(festivalOwner(festival.getId()));
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
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("현장 스태프 계정을 삭제 상태로 저장한다")
        void success_Delete_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount adminAccount = adminAccountService.save(festivalOwner(festival.getId()));
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

    private CreateFieldStaffCommand createCommand() {
        return new CreateFieldStaffCommand(
                "staff01",
                "김스태프",
                "010-1234-5678"
        );
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(
                adminAccount.getId(),
                adminAccount.getFestivalId(),
                adminAccount.getEmailValue(),
                adminAccount.getRole()
        );
    }

    private AdminAccount festivalOwner(Long festivalId) {
        return AdminAccount.createFestivalOwner(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                festivalId,
                AdminPasswordHash.of("encoded-password")
        );
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
