package com.example.chookjibupadmin.operator.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
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
import com.example.chookjibupadmin.operator.command.application.FieldStaffAccountService;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import com.example.chookjibupadmin.operator.query.application.dto.FieldStaffView;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class FieldStaffQueryApplicationServiceIntegrationTest {

    @Autowired
    private FieldStaffQueryApplicationService applicationService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Autowired
    private FestivalService festivalService;

    @Autowired
    private FieldStaffAccountService fieldStaffAccountService;

    @Nested
    @DisplayName("getFieldStaff")
    class GetFieldStaff {

        @Test
        @DisplayName("관리자가 담당 축제의 현장 스태프 목록을 조회한다")
        void success_GetFieldStaff_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = persistOwner(festival);
            fieldStaffAccountService.save(fieldStaffAccount("staff01", festival.getId()));
            fieldStaffAccountService.save(fieldStaffAccount("staff02", festival.getId()));

            // when
            List<FieldStaffView> result = applicationService.searchFieldStaff(
                    festival.getPublicId(),
                    null,
                    principal(owner)
            );

            // then
            assertThat(result)
                    .extracting(FieldStaffView::loginId)
                    .containsExactly("staff01", "staff02");
        }

        @Test
        @DisplayName("관리자가 검색어로 담당 축제의 현장 스태프 목록을 필터링한다")
        void success_GetFieldStaff_ByKeyword() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = persistOwner(festival);
            fieldStaffAccountService.save(fieldStaffAccount("staff01", "김검색", festival.getId()));
            fieldStaffAccountService.save(fieldStaffAccount("staff02", "이관리", festival.getId()));

            // when
            List<FieldStaffView> result = applicationService.searchFieldStaff(
                    festival.getPublicId(),
                    "검색",
                    principal(owner)
            );

            // then
            assertThat(result)
                    .extracting(FieldStaffView::loginId)
                    .containsExactly("staff01");
        }
    }

    @Nested
    @DisplayName("getFieldStaffById")
    class GetFieldStaffById {

        @Test
        @DisplayName("관리자가 담당 축제의 현장 스태프를 UUID로 조회한다")
        void success_GetFieldStaffById_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = persistOwner(festival);
            FieldStaffAccount fieldStaff = fieldStaffAccountService.save(
                    fieldStaffAccount("staff01", festival.getId())
            );

            // when
            FieldStaffView result = applicationService.getFieldStaff(
                    festival.getPublicId(),
                    fieldStaff.getPublicId(),
                    principal(owner)
            );

            // then
            assertThat(result.staffId()).isEqualTo(fieldStaff.getPublicId());
            assertThat(result.loginId()).isEqualTo("staff01");
        }
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
                AdminOrganization.of("마포구청 소속"),
                AdminPasswordHash.of("encoded-password")
        ));
        adminFestivalRoleService.assignFestivalOwner(owner.getId(), festival.getId());
        return owner;
    }

    private FieldStaffAccount fieldStaffAccount(String loginId, Long festivalId) {
        return fieldStaffAccount(loginId, "김스태프", festivalId);
    }

    private FieldStaffAccount fieldStaffAccount(
            String loginId,
            String name,
            Long festivalId
    ) {
        return FieldStaffAccount.create(
                festivalId,
                FieldStaffLoginId.of(loginId),
                FieldStaffName.of(name),
                FieldStaffPhoneNumber.of("010-1234-5678"),
                FieldStaffPasswordHash.of("encoded-password"),
                LocalDateTime.of(2026, 10, 9, 0, 0),
                LocalDateTime.of(2026, 10, 18, 23, 59)
        );
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
