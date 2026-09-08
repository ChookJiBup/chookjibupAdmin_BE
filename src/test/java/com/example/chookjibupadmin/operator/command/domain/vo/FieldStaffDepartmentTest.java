package com.example.chookjibupadmin.operator.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FieldStaffDepartmentTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("근무구역 앞뒤 공백을 제거한다")
        void success_Of_Trimmed() {
            // when
            FieldStaffDepartment department = FieldStaffDepartment.of("  정문 게이트  ");

            // then
            assertThat(department.getValue()).isEqualTo("정문 게이트");
        }

        @Test
        @DisplayName("근무구역이 없으면 값 없음으로 둔다")
        void success_Of_NullBoundary() {
            // when & then
            assertThat(FieldStaffDepartment.of(null).getValue()).isNull();
            assertThat(FieldStaffDepartment.of("   ").getValue()).isNull();
        }

        @Test
        @DisplayName("근무구역 100자까지 허용한다")
        void success_Of_MaxLengthBoundary() {
            // given
            String value = "가".repeat(100);

            // when
            FieldStaffDepartment department = FieldStaffDepartment.of(value);

            // then
            assertThat(department.getValue()).hasSize(100);
        }

        @Test
        @DisplayName("근무구역이 100자를 넘으면 생성할 수 없다")
        void fail_Of_TooLong_CustomException() {
            // given
            String value = "가".repeat(101);

            // when & then
            assertThatThrownBy(() -> FieldStaffDepartment.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
