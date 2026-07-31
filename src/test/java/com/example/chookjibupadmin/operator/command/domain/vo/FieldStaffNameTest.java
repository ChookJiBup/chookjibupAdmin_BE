package com.example.chookjibupadmin.operator.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FieldStaffNameTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("이름을 공백 제거 후 생성한다")
        void success_Of_Trimmed() {
            // given
            String value = " 김스태프 ";

            // when
            FieldStaffName name = FieldStaffName.of(value);

            // then
            assertThat(name.getValue()).isEqualTo("김스태프");
        }

        @Test
        @DisplayName("최대 길이 이름을 생성한다")
        void success_Of_MaxBoundary() {
            // given
            String value = "가".repeat(100);

            // when
            FieldStaffName name = FieldStaffName.of(value);

            // then
            assertThat(name.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("빈 이름은 생성할 수 없다")
        void fail_Of_Blank_CustomException() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> FieldStaffName.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("최대 길이보다 긴 이름은 생성할 수 없다")
        void fail_Of_Long_CustomException() {
            // given
            String value = "가".repeat(101);

            // when & then
            assertThatThrownBy(() -> FieldStaffName.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
