package com.example.chookjibupadmin.operator.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FestivalOperationAccessServiceIntegrationTest {

    @Autowired
    private FestivalOperationAccessService service;

    @Autowired
    private FestivalService festivalService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Autowired
    private FieldStaffAccountService fieldStaffAccountService;

    @Test
    void success_GetAuthorizedFestivalId_AdminAndFieldStaff() {
        // given
        Festival festival = festivalService.save(festival("공통 운영 축제"));
        AdminAccount admin = adminAccountService.save(adminAccount());
        adminFestivalRoleService.assignFestivalOwner(
                admin.getId(),
                festival.getId()
        );
        FieldStaffAccount fieldStaff = fieldStaffAccountService.save(
                fieldStaffAccount(festival.getId())
        );

        // when
        Long adminFestivalId = service.getAuthorizedFestivalId(
                festival.getPublicId(),
                new AdminPrincipal(admin.getId(), admin.getEmailValue())
        );
        Long fieldStaffFestivalId = service.getAuthorizedFestivalId(
                festival.getPublicId(),
                new FieldStaffPrincipal(
                        fieldStaff.getId(),
                        fieldStaff.getFestivalId(),
                        fieldStaff.getLoginIdValue(),
                        fieldStaff.getAuthVersion()
                )
        );

        // then
        assertThat(adminFestivalId).isEqualTo(festival.getId());
        assertThat(fieldStaffFestivalId).isEqualTo(festival.getId());
    }

    @Test
    void fail_GetAuthorizedFestivalId_OtherFestivalFieldStaff_CustomException() {
        // given
        Festival ownFestival = festivalService.save(festival("소속 축제", 1L));
        Festival otherFestival = festivalService.save(festival("다른 축제", 2L));
        FieldStaffAccount fieldStaff = fieldStaffAccountService.save(
                fieldStaffAccount(ownFestival.getId())
        );
        FieldStaffPrincipal principal = new FieldStaffPrincipal(
                fieldStaff.getId(),
                fieldStaff.getFestivalId(),
                fieldStaff.getLoginIdValue(),
                fieldStaff.getAuthVersion()
        );

        // when & then
        assertThatThrownBy(() -> service.getAuthorizedFestivalId(
                otherFestival.getPublicId(),
                principal
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FORBIDDEN.getMessage());
    }

    private AdminAccount adminAccount() {
        return AdminAccount.createAdmin(
                AdminEmail.of("operation-admin@mapo.go.kr"),
                AdminName.of("운영관리자"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("password-hash")
        );
    }

    private FieldStaffAccount fieldStaffAccount(Long festivalId) {
        return FieldStaffAccount.create(
                festivalId,
                FieldStaffLoginId.of("operation01"),
                FieldStaffName.of("현장스태프"),
                FieldStaffPhoneNumber.of("010-1234-5678"),
                FieldStaffPasswordHash.of("password-hash"),
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 31, 23, 59)
        );
    }

    private Festival festival(String name) {
        return festival(name, 1L);
    }

    private Festival festival(String name, Long seriesId) {
        return Festival.create(
                seriesId,
                UUID.randomUUID(),
                FestivalName.of(name),
                FestivalDescription.of("현장 운영 인증 테스트"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(9, 0),
                        LocalTime.of(22, 0)
                )
        );
    }
}
