package com.example.chookjibupadmin.operator.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccountRepository;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FieldStaffAccountServiceTest {

    @InjectMocks
    private FieldStaffAccountService fieldStaffAccountService;

    @Mock
    private FieldStaffAccountRepository fieldStaffAccountRepository;

    @Nested
    @DisplayName("getByPublicId")
    class GetByPublicId {

        @Test
        @DisplayName("외부 UUID로 현장 스태프 계정을 조회한다")
        void success_GetByPublicId() {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            given(fieldStaffAccountRepository.findByPublicId(account.getPublicId()))
                    .willReturn(Optional.of(account));

            // when
            FieldStaffAccount found = fieldStaffAccountService.getByPublicId(
                    account.getPublicId()
            );

            // then
            assertThat(found).isSameAs(account);
        }

        @Test
        @DisplayName("현장 스태프 계정이 없으면 예외를 던진다")
        void fail_GetByPublicId_CustomException() {
            // given
            UUID publicId = UUID.randomUUID();
            given(fieldStaffAccountRepository.findByPublicId(publicId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> fieldStaffAccountService.getByPublicId(publicId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FIELD_STAFF_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("getAllByPublicIds")
    class GetAllByPublicIds {

        @Test
        @DisplayName("외부 UUID 목록에 해당하는 현장 스태프 계정을 모두 조회한다")
        void success_GetAllByPublicIds() {
            // given
            FieldStaffAccount first = fieldStaffAccount("staff01");
            FieldStaffAccount second = fieldStaffAccount("staff02");
            List<UUID> publicIds = List.of(first.getPublicId(), second.getPublicId());
            given(fieldStaffAccountRepository.findAllByPublicIdIn(publicIds))
                    .willReturn(List.of(first, second));

            // when
            List<FieldStaffAccount> found =
                    fieldStaffAccountService.getAllByPublicIds(publicIds);

            // then
            assertThat(found).containsExactly(first, second);
        }

        @Test
        @DisplayName("요청한 계정이 하나라도 없으면 예외를 던진다")
        void fail_GetAllByPublicIds_CustomException() {
            // given
            FieldStaffAccount account = fieldStaffAccount("staff01");
            List<UUID> publicIds = List.of(account.getPublicId(), UUID.randomUUID());
            given(fieldStaffAccountRepository.findAllByPublicIdIn(publicIds))
                    .willReturn(List.of(account));

            // when & then
            assertThatThrownBy(() ->
                    fieldStaffAccountService.getAllByPublicIds(publicIds))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FIELD_STAFF_NOT_FOUND.getMessage());
        }
    }

    private FieldStaffAccount fieldStaffAccount() {
        return fieldStaffAccount("staff01");
    }

    private FieldStaffAccount fieldStaffAccount(String loginId) {
        return FieldStaffAccount.create(
                1L,
                FieldStaffLoginId.of(loginId),
                FieldStaffName.of("김스태프"),
                FieldStaffPhoneNumber.of("010-1234-5678"),
                FieldStaffPasswordHash.of("encoded-password"),
                LocalDateTime.of(2026, 10, 9, 0, 0),
                LocalDateTime.of(2026, 10, 18, 23, 59)
        );
    }
}
