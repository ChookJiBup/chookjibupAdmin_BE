package com.example.chookjibupadmin.operator.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FieldStaffPhoneNumberTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("하이픈이 있는 전화번호를 생성한다")
        void success_Of_WithHyphen() {
            // given
            String value = "010-1234-5678";

            // when
            FieldStaffPhoneNumber phoneNumber = FieldStaffPhoneNumber.of(value);

            // then
            assertThat(phoneNumber.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("하이픈이 없는 최소 길이 전화번호를 생성한다")
        void success_Of_MinBoundary() {
            // given
            String value = "0101234567";

            // when
            FieldStaffPhoneNumber phoneNumber = FieldStaffPhoneNumber.of(value);

            // then
            assertThat(phoneNumber.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("하이픈이 없는 최대 길이 전화번호를 생성한다")
        void success_Of_MaxBoundary() {
            // given
            String value = "01012345678";

            // when
            FieldStaffPhoneNumber phoneNumber = FieldStaffPhoneNumber.of(value);

            // then
            assertThat(phoneNumber.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("휴대전화 형식이 아니면 생성할 수 없다")
        void fail_Of_InvalidFormat_CustomException() {
            // given
            String value = "02-1234-5678";

            // when & then
            assertThatThrownBy(() -> FieldStaffPhoneNumber.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
