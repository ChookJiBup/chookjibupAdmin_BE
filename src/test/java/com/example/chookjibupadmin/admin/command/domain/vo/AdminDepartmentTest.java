package com.example.chookjibupadmin.admin.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminDepartmentTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("JPA 기본 생성자로 생성할 수 있다")
        void success_Constructor_ForJpa() {
            // given

            // when
            AdminDepartment department = new AdminDepartment();

            // then
            assertThat(department.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("부서명 앞뒤 공백을 제거한다")
        void success_Of_Normalized() {
            // given
            String value = " 관광정책과 ";

            // when
            AdminDepartment department = AdminDepartment.of(value);

            // then
            assertThat(department.getValue()).isEqualTo("관광정책과");
        }

        @Test
        @DisplayName("최소 길이 경계값인 두 글자를 허용한다")
        void success_Of_MinLengthBoundary() {
            // given
            String value = "관광";

            // when
            AdminDepartment department = AdminDepartment.of(value);

            // then
            assertThat(department.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("최대 길이 경계값인 100자를 허용한다")
        void success_Of_MaxLengthBoundary() {
            // given
            String value = "가".repeat(100);

            // when
            AdminDepartment department = AdminDepartment.of(value);

            // then
            assertThat(department.getValue()).hasSize(100);
        }

        @Test
        @DisplayName("null 부서명은 거절한다")
        void fail_Of_Null_CustomException() {
            // given
            String value = null;

            // when & then
            assertThatThrownBy(() -> AdminDepartment.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("빈 부서명은 거절한다")
        void fail_Of_Blank_CustomException() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> AdminDepartment.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("100자를 초과한 부서명은 거절한다")
        void fail_Of_OverMaxLength_CustomException() {
            // given
            String value = "가".repeat(101);

            // when & then
            assertThatThrownBy(() -> AdminDepartment.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
