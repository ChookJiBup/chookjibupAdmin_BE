package com.example.chookjibupadmin.operator.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FieldStaffAccountTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("현장 스태프 계정을 생성한다")
        void success_Create() {
            // given
            LocalDateTime validFrom = LocalDateTime.of(2026, 10, 9, 0, 0);
            LocalDateTime validUntil = LocalDateTime.of(2026, 10, 18, 23, 59);

            // when
            FieldStaffAccount account = fieldStaffAccount(validFrom, validUntil);

            // then
            assertThat(account.getPublicId()).isNotNull();
            assertThat(account.getFestivalId()).isEqualTo(1L);
            assertThat(account.getLoginIdValue()).isEqualTo("staff01");
            assertThat(account.getStatus()).isEqualTo(FieldStaffStatus.ACTIVE);
        }

        @Test
        @DisplayName("유효기간 시작과 종료가 같아도 생성한다")
        void success_Create_SameValidTimeBoundary() {
            // given
            LocalDateTime validTime = LocalDateTime.of(2026, 10, 9, 0, 0);

            // when
            FieldStaffAccount account = fieldStaffAccount(validTime, validTime);

            // then
            assertThat(account.getValidFrom()).isEqualTo(validTime);
            assertThat(account.getValidUntil()).isEqualTo(validTime);
        }

        @Test
        @DisplayName("축제 ID가 없으면 생성할 수 없다")
        void fail_Create_NullFestivalId_CustomException() {
            // given
            LocalDateTime validFrom = LocalDateTime.of(2026, 10, 9, 0, 0);
            LocalDateTime validUntil = LocalDateTime.of(2026, 10, 18, 23, 59);

            // when & then
            assertThatThrownBy(() -> FieldStaffAccount.create(
                    null,
                    FieldStaffLoginId.of("staff01"),
                    FieldStaffName.of("김스태프"),
                    FieldStaffPhoneNumber.of("010-1234-5678"),
                    FieldStaffPasswordHash.of("encoded-password"),
                    validFrom,
                    validUntil
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("유효 시작 시각이 종료 시각보다 늦으면 생성할 수 없다")
        void fail_Create_InvalidValidPeriod_CustomException() {
            // given
            LocalDateTime validFrom = LocalDateTime.of(2026, 10, 19, 0, 0);
            LocalDateTime validUntil = LocalDateTime.of(2026, 10, 18, 23, 59);

            // when & then
            assertThatThrownBy(() -> fieldStaffAccount(validFrom, validUntil))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    @Nested
    @DisplayName("isUsableAt")
    class IsUsableAt {

        @Test
        @DisplayName("유효기간 시작 시각에는 사용할 수 있다")
        void success_IsUsableAt_StartBoundary() {
            // given
            LocalDateTime validFrom = LocalDateTime.of(2026, 10, 9, 0, 0);
            FieldStaffAccount account = fieldStaffAccount(
                    validFrom,
                    LocalDateTime.of(2026, 10, 18, 23, 59)
            );

            // when
            boolean usable = account.isUsableAt(validFrom);

            // then
            assertThat(usable).isTrue();
        }

        @Test
        @DisplayName("유효기간 종료 시각에는 사용할 수 있다")
        void success_IsUsableAt_EndBoundary() {
            // given
            LocalDateTime validUntil = LocalDateTime.of(2026, 10, 18, 23, 59);
            FieldStaffAccount account = fieldStaffAccount(
                    LocalDateTime.of(2026, 10, 9, 0, 0),
                    validUntil
            );

            // when
            boolean usable = account.isUsableAt(validUntil);

            // then
            assertThat(usable).isTrue();
        }

        @Test
        @DisplayName("유효기간 전이면 사용할 수 없다")
        void success_IsUsableAt_BeforeStartBoundary() {
            // given
            LocalDateTime validFrom = LocalDateTime.of(2026, 10, 9, 0, 0);
            FieldStaffAccount account = fieldStaffAccount(
                    validFrom,
                    LocalDateTime.of(2026, 10, 18, 23, 59)
            );

            // when
            boolean usable = account.isUsableAt(validFrom.minusNanos(1));

            // then
            assertThat(usable).isFalse();
        }

        @Test
        @DisplayName("삭제된 계정은 유효기간 안이어도 사용할 수 없다")
        void success_IsUsableAt_Deleted() {
            // given
            LocalDateTime now = LocalDateTime.of(2026, 10, 10, 0, 0);
            FieldStaffAccount account = fieldStaffAccount(
                    LocalDateTime.of(2026, 10, 9, 0, 0),
                    LocalDateTime.of(2026, 10, 18, 23, 59)
            );
            account.delete();

            // when
            boolean usable = account.isUsableAt(now);

            // then
            assertThat(usable).isFalse();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("현장 스태프 계정을 삭제 상태로 변경한다")
        void success_Delete() {
            // given
            FieldStaffAccount account = fieldStaffAccount();

            // when
            account.delete();

            // then
            assertThat(account.getStatus()).isEqualTo(FieldStaffStatus.DELETED);
            assertThat(account.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("이미 삭제된 계정은 다시 삭제할 수 없다")
        void fail_Delete_CustomException() {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            account.delete();

            // when & then
            assertThatThrownBy(account::delete)
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FIELD_STAFF_NOT_ACTIVE.getMessage());
        }
    }

    private FieldStaffAccount fieldStaffAccount() {
        return fieldStaffAccount(
                LocalDateTime.of(2026, 10, 9, 0, 0),
                LocalDateTime.of(2026, 10, 18, 23, 59)
        );
    }

    private FieldStaffAccount fieldStaffAccount(
            LocalDateTime validFrom,
            LocalDateTime validUntil
    ) {
        return FieldStaffAccount.create(
                1L,
                FieldStaffLoginId.of("staff01"),
                FieldStaffName.of("김스태프"),
                FieldStaffPhoneNumber.of("010-1234-5678"),
                FieldStaffPasswordHash.of("encoded-password"),
                validFrom,
                validUntil
        );
    }
}
