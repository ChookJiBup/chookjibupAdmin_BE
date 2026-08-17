package com.example.chookjibupadmin.admin.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.domain.AccountKind;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.infrastructure.FieldStaffPasswordGenerator;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminOperatorRegistrationServiceTest {

    @InjectMocks
    private AdminOperatorRegistrationService service;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService roleService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FieldStaffPasswordGenerator passwordGenerator;

    @Test
    @DisplayName("미가입 외부업자 이메일이면 계정을 생성하고 운영자로 배정한다")
    void success_Register_CreateContractor() {
        // given
        UUID festivalId = UUID.randomUUID();
        AdminAccount owner = governmentOwner();
        Festival festival = festival(festivalId, 10L);
        AdminFestivalRole ownerRole = org.mockito.Mockito.mock(AdminFestivalRole.class);

        given(adminAccountService.getById(1L)).willReturn(owner);
        given(festivalService.getByPublicId(festivalId)).willReturn(festival);
        given(roleService.getByAdminAccountIdAndFestivalId(1L, 10L)).willReturn(ownerRole);
        given(ownerRole.canInviteSubAdmin()).willReturn(true);
        given(adminAccountService.findByEmail(AdminEmail.of(
                "vendor@gmail.com",
                AccountKind.CONTRACTOR
        ))).willReturn(Optional.empty());
        given(passwordGenerator.generate()).willReturn("TempPass!123");
        given(passwordEncoder.encode("TempPass!123")).willReturn("encoded");
        given(adminAccountService.save(any(AdminAccount.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = service.register(
                festivalId,
                "vendor@gmail.com",
                "김운영",
                "축제기획(주)",
                new AdminPrincipal(1L, owner.getEmailValue())
        );

        // then
        assertThat(result.created()).isTrue();
        assertThat(result.temporaryPassword()).isEqualTo("TempPass!123");
        then(roleService).should().assignSubAdmin(any(), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(1L));
    }

    @Test
    @DisplayName("기존 외부업자 계정이면 운영자로만 배정한다")
    void success_Register_AssignExistingContractor() {
        // given
        UUID festivalId = UUID.randomUUID();
        AdminAccount owner = governmentOwner();
        AdminAccount contractor = AdminAccount.createContractor(
                AdminEmail.of("vendor@gmail.com"),
                AdminName.of("김운영"),
                AdminOrganization.of("축제기획(주)"),
                AdminPasswordHash.of("encoded")
        );
        ReflectionTestUtils.setField(contractor, "id", 2L);
        Festival festival = festival(festivalId, 10L);
        AdminFestivalRole ownerRole = org.mockito.Mockito.mock(AdminFestivalRole.class);

        given(adminAccountService.getById(1L)).willReturn(owner);
        given(festivalService.getByPublicId(festivalId)).willReturn(festival);
        given(roleService.getByAdminAccountIdAndFestivalId(1L, 10L)).willReturn(ownerRole);
        given(ownerRole.canInviteSubAdmin()).willReturn(true);
        given(adminAccountService.findByEmail(AdminEmail.of(
                "vendor@gmail.com",
                AccountKind.CONTRACTOR
        ))).willReturn(Optional.of(contractor));

        // when
        var result = service.register(
                festivalId,
                "vendor@gmail.com",
                "김운영",
                "축제기획(주)",
                new AdminPrincipal(1L, owner.getEmailValue())
        );

        // then
        assertThat(result.created()).isFalse();
        assertThat(result.temporaryPassword()).isNull();
        then(roleService).should().assignSubAdmin(2L, 10L, 1L);
        then(adminAccountService).should(org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("공무원 계정 이메일이면 운영자로 등록할 수 없다")
    void fail_Register_GovernmentEmail() {
        // given
        UUID festivalId = UUID.randomUUID();
        AdminAccount owner = governmentOwner();
        Festival festival = festival(festivalId, 10L);
        AdminFestivalRole ownerRole = org.mockito.Mockito.mock(AdminFestivalRole.class);

        given(adminAccountService.getById(1L)).willReturn(owner);
        given(festivalService.getByPublicId(festivalId)).willReturn(festival);
        given(roleService.getByAdminAccountIdAndFestivalId(1L, 10L)).willReturn(ownerRole);
        given(ownerRole.canInviteSubAdmin()).willReturn(true);

        // when & then
        assertThatThrownBy(() -> service.register(
                festivalId,
                "admin@mapo.go.kr",
                "홍길동",
                "관광정책과",
                new AdminPrincipal(1L, owner.getEmailValue())
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(
                        ErrorCode.AUTH_CONTRACTOR_GOVERNMENT_EMAIL_NOT_ALLOWED.getMessage()
                );
    }

    @Test
    @DisplayName("운영자는 운영자를 등록할 수 없다")
    void fail_Register_SubAdminForbidden() {
        // given
        UUID festivalId = UUID.randomUUID();
        AdminAccount operator = AdminAccount.createContractor(
                AdminEmail.of("operator@gmail.com"),
                AdminName.of("김운영"),
                AdminOrganization.of("축제기획(주)"),
                AdminPasswordHash.of("encoded")
        );
        ReflectionTestUtils.setField(operator, "id", 3L);
        Festival festival = festival(festivalId, 10L);
        AdminFestivalRole operatorRole = org.mockito.Mockito.mock(AdminFestivalRole.class);

        given(adminAccountService.getById(3L)).willReturn(operator);
        given(festivalService.getByPublicId(festivalId)).willReturn(festival);
        given(roleService.getByAdminAccountIdAndFestivalId(3L, 10L)).willReturn(operatorRole);
        given(operatorRole.canInviteSubAdmin()).willReturn(false);

        // when & then
        assertThatThrownBy(() -> service.register(
                festivalId,
                "vendor@gmail.com",
                "다른업체",
                "다른기획(주)",
                new AdminPrincipal(3L, operator.getEmailValue())
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FORBIDDEN.getMessage());
        then(adminAccountService).should(org.mockito.Mockito.never()).save(any());
    }

    private AdminAccount governmentOwner() {
        AdminAccount owner = AdminAccount.createGovernment(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("총괄"),
                AdminOrganization.of("관광정책과"),
                com.example.chookjibupadmin.admin.command.domain.vo.AdminRank.of("과장"),
                AdminPasswordHash.of("encoded")
        );
        ReflectionTestUtils.setField(owner, "id", 1L);
        return owner;
    }

    private Festival festival(UUID publicId, Long id) {
        Festival festival = org.mockito.Mockito.mock(Festival.class);
        given(festival.getId()).willReturn(id);
        return festival;
    }
}
