package com.example.chookjibupadmin.operator.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
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
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.application.dto.CreateFieldStaffCommand;
import com.example.chookjibupadmin.operator.command.application.dto.CreateFieldStaffResult;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffStatus;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.infrastructure.FieldStaffPasswordGenerator;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FieldStaffManagementServiceTest {

    @InjectMocks
    private FieldStaffManagementService service;

    @Mock
    private FieldStaffAccountService fieldStaffAccountService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FieldStaffPasswordGenerator passwordGenerator;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("1관리자가 담당 축제의 현장 스태프 계정을 생성한다")
        void success_Create_FestivalOwner() {
            // given
            Festival festival = festival(1L);
            AdminAccount adminAccount = festivalOwner(1L);
            CreateFieldStaffCommand command = createCommand();
            given(adminAccountService.getById(1L)).willReturn(adminAccount);
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(fieldStaffAccountService.existsByFestivalIdAndLoginId(
                    1L,
                    FieldStaffLoginId.of("staff01")
            )).willReturn(false);
            given(passwordGenerator.generate()).willReturn("TempPass123!");
            given(passwordEncoder.encode("TempPass123!")).willReturn("encoded-password");
            given(fieldStaffAccountService.save(any(FieldStaffAccount.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            CreateFieldStaffResult result = service.create(
                    festival.getPublicId(),
                    command,
                    principal(AdminRole.FESTIVAL_OWNER)
            );

            // then
            assertThat(result.temporaryPassword()).isEqualTo("TempPass123!");
            assertThat(result.fieldStaffAccount().getValidFrom())
                    .isEqualTo(LocalDate.of(2026, 10, 9).atStartOfDay());
            assertThat(result.fieldStaffAccount().getValidUntil())
                    .isEqualTo(LocalDate.of(2026, 10, 18).atTime(LocalTime.MAX));

            ArgumentCaptor<FieldStaffAccount> captor =
                    ArgumentCaptor.forClass(FieldStaffAccount.class);
            then(fieldStaffAccountService).should().save(captor.capture());
            assertThat(captor.getValue().getPasswordHashValue()).isEqualTo("encoded-password");
        }

        @Test
        @DisplayName("2관리자가 담당 축제의 현장 스태프 계정을 생성한다")
        void success_Create_SubAdmin() {
            // given
            Festival festival = festival(1L);
            AdminAccount adminAccount = subAdmin(1L);
            CreateFieldStaffCommand command = createCommand();
            given(adminAccountService.getById(1L)).willReturn(adminAccount);
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(fieldStaffAccountService.existsByFestivalIdAndLoginId(
                    1L,
                    FieldStaffLoginId.of("staff01")
            )).willReturn(false);
            given(passwordGenerator.generate()).willReturn("TempPass123!");
            given(passwordEncoder.encode("TempPass123!")).willReturn("encoded-password");
            given(fieldStaffAccountService.save(any(FieldStaffAccount.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            CreateFieldStaffResult result = service.create(
                    festival.getPublicId(),
                    command,
                    principal(AdminRole.SUB_ADMIN)
            );

            // then
            assertThat(result.fieldStaffAccount().getLoginIdValue()).isEqualTo("staff01");
        }

        @Test
        @DisplayName("같은 축제 안에서 중복 아이디는 생성할 수 없다")
        void fail_Create_DuplicatedLoginId_CustomException() {
            // given
            Festival festival = festival(1L);
            given(adminAccountService.getById(1L))
                    .willReturn(festivalOwner(1L));
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(fieldStaffAccountService.existsByFestivalIdAndLoginId(
                    1L,
                    FieldStaffLoginId.of("staff01")
            )).willReturn(true);

            // when & then
            assertThatThrownBy(() -> service.create(
                    festival.getPublicId(),
                    createCommand(),
                    principal(AdminRole.FESTIVAL_OWNER)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FIELD_STAFF_LOGIN_ID_DUPLICATED.getMessage());
        }

        @Test
        @DisplayName("담당 축제가 아니면 생성할 수 없다")
        void fail_Create_DifferentFestival_CustomException() {
            // given
            Festival festival = festival(1L);
            given(adminAccountService.getById(1L))
                    .willReturn(festivalOwner(2L));
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);

            // when & then
            assertThatThrownBy(() -> service.create(
                    festival.getPublicId(),
                    createCommand(),
                    principal(AdminRole.FESTIVAL_OWNER)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FORBIDDEN.getMessage());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("담당 축제의 현장 스태프 계정을 삭제한다")
        void success_Delete() {
            // given
            Festival festival = festival(1L);
            FieldStaffAccount account = fieldStaffAccount(1L);
            given(adminAccountService.getById(1L))
                    .willReturn(subAdmin(1L));
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(fieldStaffAccountService.getByPublicId(account.getPublicId()))
                    .willReturn(account);

            // when
            service.delete(
                    festival.getPublicId(),
                    account.getPublicId(),
                    principal(AdminRole.SUB_ADMIN)
            );

            // then
            assertThat(account.getStatus()).isEqualTo(FieldStaffStatus.DELETED);
        }

        @Test
        @DisplayName("다른 축제의 현장 스태프 계정은 삭제할 수 없다")
        void fail_Delete_FieldStaffNotFound_CustomException() {
            // given
            Festival festival = festival(1L);
            FieldStaffAccount account = fieldStaffAccount(2L);
            given(adminAccountService.getById(1L))
                    .willReturn(festivalOwner(1L));
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(fieldStaffAccountService.getByPublicId(account.getPublicId()))
                    .willReturn(account);

            // when & then
            assertThatThrownBy(() -> service.delete(
                    festival.getPublicId(),
                    account.getPublicId(),
                    principal(AdminRole.FESTIVAL_OWNER)
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FIELD_STAFF_NOT_FOUND.getMessage());
        }
    }

    private CreateFieldStaffCommand createCommand() {
        return new CreateFieldStaffCommand(
                "staff01",
                "김스태프",
                "010-1234-5678"
        );
    }

    private AdminPrincipal principal(AdminRole role) {
        return new AdminPrincipal(1L, 1L, "owner@mapo.go.kr", role);
    }

    private AdminAccount festivalOwner(Long festivalId) {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminPasswordHash.of("encoded-password")
        );
        adminAccount.assignFestivalOwner(festivalId);
        return adminAccount;
    }

    private AdminAccount subAdmin(Long festivalId) {
        return AdminAccount.createSubAdmin(
                AdminEmail.of("sub@mapo.go.kr"),
                AdminName.of("김서브"),
                AdminOrganization.of("마포구청 소속"),
                festivalId,
                AdminPasswordHash.of("encoded-password"),
                1L
        );
    }

    private Festival festival(Long festivalId) {
        Festival festival = Festival.create(
                10L,
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

    private FieldStaffAccount fieldStaffAccount(Long festivalId) {
        FieldStaffAccount account = FieldStaffAccount.create(
                festivalId,
                FieldStaffLoginId.of("staff01"),
                com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName.of("김스태프"),
                com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber.of("010-1234-5678"),
                com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash.of("encoded-password"),
                LocalDate.of(2026, 10, 9).atStartOfDay(),
                LocalDate.of(2026, 10, 18).atTime(LocalTime.MAX)
        );
        ReflectionTestUtils.setField(account, "id", 1L);
        return account;
    }
}
