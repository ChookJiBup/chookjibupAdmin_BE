package com.example.chookjibupadmin.operator.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminDepartment;
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
import com.example.chookjibupadmin.operator.command.application.dto.CreateFieldStaffCommand;
import com.example.chookjibupadmin.operator.command.application.dto.CreateFieldStaffResult;
import com.example.chookjibupadmin.operator.command.application.dto.UpdateFieldStaffCommand;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffStatus;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.infrastructure.FieldStaffPasswordGenerator;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
    private AdminFestivalRoleService adminFestivalRoleService;

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
            AdminAccount adminAccount = adminAccount();
            CreateFieldStaffCommand command = createCommand();
            given(adminAccountService.getById(1L)).willReturn(adminAccount);
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            givenManageRole(festival, AdminRole.FESTIVAL_OWNER);
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
                    principal()
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
            AdminAccount adminAccount = adminAccount();
            CreateFieldStaffCommand command = createCommand();
            given(adminAccountService.getById(1L)).willReturn(adminAccount);
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            givenManageRole(festival, AdminRole.SUB_ADMIN);
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
                    principal()
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
                    .willReturn(adminAccount());
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            givenManageRole(festival, AdminRole.FESTIVAL_OWNER);
            given(fieldStaffAccountService.existsByFestivalIdAndLoginId(
                    1L,
                    FieldStaffLoginId.of("staff01")
            )).willReturn(true);

            // when & then
            assertThatThrownBy(() -> service.create(
                    festival.getPublicId(),
                    createCommand(),
                    principal()
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
                    .willReturn(adminAccount());
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    1L,
                    festival.getId()
            )).willThrow(new CustomException(ErrorCode.FORBIDDEN));

            // when & then
            assertThatThrownBy(() -> service.create(
                    festival.getPublicId(),
                    createCommand(),
                    principal()
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
                    .willReturn(adminAccount());
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            givenManageRole(festival, AdminRole.SUB_ADMIN);
            given(fieldStaffAccountService.getByPublicId(account.getPublicId()))
                    .willReturn(account);

            // when
            service.delete(
                    festival.getPublicId(),
                    account.getPublicId(),
                    principal()
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
                    .willReturn(adminAccount());
            given(festivalService.getByPublicId(festival.getPublicId()))
                    .willReturn(festival);
            givenManageRole(festival, AdminRole.FESTIVAL_OWNER);
            given(fieldStaffAccountService.getByPublicId(account.getPublicId()))
                    .willReturn(account);

            // when & then
            assertThatThrownBy(() -> service.delete(
                    festival.getPublicId(),
                    account.getPublicId(),
                    principal()
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FIELD_STAFF_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("deleteAll")
    class DeleteAll {

        @Test
        @DisplayName("담당 축제의 현장 스태프 계정을 여러 개 삭제한다")
        void success_DeleteAll() {
            // given
            Festival festival = festival(1L);
            FieldStaffAccount first = fieldStaffAccount(1L, "staff01");
            FieldStaffAccount second = fieldStaffAccount(1L, "staff02");
            List<UUID> publicIds = List.of(first.getPublicId(), second.getPublicId());
            givenManagePermission(festival);
            given(fieldStaffAccountService.getAllByPublicIds(any()))
                    .willReturn(List.of(first, second));

            // when
            service.deleteAll(
                    festival.getPublicId(),
                    publicIds,
                    principal()
            );

            // then
            assertThat(first.getStatus()).isEqualTo(FieldStaffStatus.DELETED);
            assertThat(second.getStatus()).isEqualTo(FieldStaffStatus.DELETED);
        }

        @Test
        @DisplayName("삭제 대상이 비어 있으면 요청을 거절한다")
        void fail_DeleteAll_InvalidRequest_CustomException() {
            // given
            Festival festival = festival(1L);
            givenManagePermission(festival);

            // when & then
            assertThatThrownBy(() -> service.deleteAll(
                    festival.getPublicId(),
                    List.of(),
                    principal()
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("삭제 대상 UUID가 중복되면 요청을 거절한다")
        void fail_DeleteAll_DuplicatedId_CustomException() {
            // given
            Festival festival = festival(1L);
            UUID publicId = UUID.randomUUID();
            givenManagePermission(festival);

            // when & then
            assertThatThrownBy(() -> service.deleteAll(
                    festival.getPublicId(),
                    List.of(publicId, publicId),
                    principal()
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("다른 축제 계정이 섞이면 어떤 계정도 삭제하지 않는다")
        void fail_DeleteAll_DifferentFestival_CustomException() {
            // given
            Festival festival = festival(1L);
            FieldStaffAccount sameFestival = fieldStaffAccount(1L, "staff01");
            FieldStaffAccount otherFestival = fieldStaffAccount(2L, "staff02");
            List<UUID> publicIds = List.of(
                    sameFestival.getPublicId(),
                    otherFestival.getPublicId()
            );
            givenManagePermission(festival);
            given(fieldStaffAccountService.getAllByPublicIds(any()))
                    .willReturn(List.of(sameFestival, otherFestival));

            // when & then
            assertThatThrownBy(() -> service.deleteAll(
                    festival.getPublicId(),
                    publicIds,
                    principal()
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FIELD_STAFF_NOT_FOUND.getMessage());
            assertThat(sameFestival.getStatus()).isEqualTo(FieldStaffStatus.ACTIVE);
            assertThat(otherFestival.getStatus()).isEqualTo(FieldStaffStatus.ACTIVE);
        }

        @Test
        @DisplayName("이미 삭제된 계정이 섞이면 어떤 계정도 변경하지 않는다")
        void fail_DeleteAll_DeletedAccount_CustomException() {
            // given
            Festival festival = festival(1L);
            FieldStaffAccount active = fieldStaffAccount(1L, "staff01");
            FieldStaffAccount deleted = fieldStaffAccount(1L, "staff02");
            deleted.delete();
            List<UUID> publicIds = List.of(active.getPublicId(), deleted.getPublicId());
            givenManagePermission(festival);
            given(fieldStaffAccountService.getAllByPublicIds(any()))
                    .willReturn(List.of(active, deleted));

            // when & then
            assertThatThrownBy(() -> service.deleteAll(
                    festival.getPublicId(),
                    publicIds,
                    principal()
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FIELD_STAFF_NOT_ACTIVE.getMessage());
            assertThat(active.getStatus()).isEqualTo(FieldStaffStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("manage")
    class Manage {

        @Test
        @DisplayName("담당 축제 스태프 정보를 수정한다")
        void success_Update() {
            Festival festival = festival(1L);
            FieldStaffAccount account = fieldStaffAccount(1L);
            givenManagePermission(festival);
            given(fieldStaffAccountService.getByPublicId(account.getPublicId()))
                    .willReturn(account);

            service.update(
                    festival.getPublicId(),
                    account.getPublicId(),
                    new UpdateFieldStaffCommand("박스태프", "010-9999-8888"),
                    principal()
            );

            assertThat(account.getNameValue()).isEqualTo("박스태프");
            assertThat(account.getPhoneNumberValue()).isEqualTo("010-9999-8888");
        }

        @Test
        @DisplayName("담당 축제 스태프 임시 비밀번호를 재발급한다")
        void success_ReissuePassword() {
            Festival festival = festival(1L);
            FieldStaffAccount account = fieldStaffAccount(1L);
            givenManagePermission(festival);
            given(fieldStaffAccountService.getByPublicId(account.getPublicId()))
                    .willReturn(account);
            given(passwordGenerator.generate()).willReturn("1234567890123456");
            given(passwordEncoder.encode("1234567890123456"))
                    .willReturn("new-hash");

            String result = service.reissuePassword(
                    festival.getPublicId(),
                    account.getPublicId(),
                    principal()
            );

            assertThat(result).isEqualTo("1234567890123456");
            assertThat(account.getPasswordHashValue()).isEqualTo("new-hash");
        }

        @Test
        @DisplayName("담당 축제 스태프를 비활성화한 뒤 다시 활성화한다")
        void success_ChangeActiveStatus() {
            Festival festival = festival(1L);
            FieldStaffAccount account = fieldStaffAccount(1L);
            givenManagePermission(festival);
            given(fieldStaffAccountService.getByPublicId(account.getPublicId()))
                    .willReturn(account);

            service.changeActiveStatus(
                    festival.getPublicId(),
                    account.getPublicId(),
                    false,
                    principal()
            );
            assertThat(account.getStatus()).isEqualTo(FieldStaffStatus.INACTIVE);

            service.changeActiveStatus(
                    festival.getPublicId(),
                    account.getPublicId(),
                    true,
                    principal()
            );
            assertThat(account.getStatus()).isEqualTo(FieldStaffStatus.ACTIVE);
        }

        @Test
        @DisplayName("다른 축제 스태프의 상태는 변경할 수 없다")
        void fail_ChangeActiveStatus_DifferentFestival_CustomException() {
            Festival festival = festival(1L);
            FieldStaffAccount account = fieldStaffAccount(2L);
            givenManagePermission(festival);
            given(fieldStaffAccountService.getByPublicId(account.getPublicId()))
                    .willReturn(account);

            assertThatThrownBy(() -> service.changeActiveStatus(
                    festival.getPublicId(),
                    account.getPublicId(),
                    false,
                    principal()
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FIELD_STAFF_NOT_FOUND.getMessage());
            assertThat(account.getStatus()).isEqualTo(FieldStaffStatus.ACTIVE);
        }
    }

    private CreateFieldStaffCommand createCommand() {
        return new CreateFieldStaffCommand(
                "staff01",
                "김스태프",
                "010-1234-5678"
        );
    }

    private AdminPrincipal principal() {
        return new AdminPrincipal(1L, "owner@mapo.go.kr");
    }

    private void givenManagePermission(Festival festival) {
        given(adminAccountService.getById(1L)).willReturn(adminAccount());
        given(festivalService.getByPublicId(festival.getPublicId())).willReturn(festival);
        givenManageRole(festival, AdminRole.FESTIVAL_OWNER);
    }

    private void givenManageRole(Festival festival, AdminRole role) {
        AdminFestivalRole festivalRole = role == AdminRole.FESTIVAL_OWNER
                ? AdminFestivalRole.createFestivalOwner(1L, festival.getId())
                : AdminFestivalRole.createSubAdmin(1L, festival.getId(), 2L);
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                1L,
                festival.getId()
        )).willReturn(festivalRole);
    }

    private AdminAccount adminAccount() {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminDepartment.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(adminAccount, "id", 1L);
        return adminAccount;
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
        return fieldStaffAccount(festivalId, "staff01");
    }

    private FieldStaffAccount fieldStaffAccount(Long festivalId, String loginId) {
        FieldStaffAccount account = FieldStaffAccount.create(
                festivalId,
                FieldStaffLoginId.of(loginId),
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
