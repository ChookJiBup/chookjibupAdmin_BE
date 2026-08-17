package com.example.chookjibupadmin.api.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminSignupRequestTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Nested
    @DisplayName("employee info validation")
    class EmployeeInfoValidation {

        @Test
        @DisplayName("과·팀과 직급의 최소 길이 경계값을 허용한다")
        void success_EmployeeInfo_MinBoundary() {
            // given
            AdminSignupRequest request = request("관광", "과");

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("과·팀과 직급의 최대 길이 경계값을 허용한다")
        void success_EmployeeInfo_MaxBoundary() {
            // given
            AdminSignupRequest request = request(
                    "가".repeat(255),
                    "나".repeat(50)
            );

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("빈 직급은 거절한다")
        void fail_Rank_Blank_ConstraintViolation() {
            // given
            AdminSignupRequest request = request("관광정책과", " ");

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("50자를 초과한 직급은 거절한다")
        void fail_Rank_OverMax_ConstraintViolation() {
            // given
            AdminSignupRequest request = request("관광정책과", "가".repeat(51));

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }
    }

    private AdminSignupRequest request(String organization, String rank) {
        return new AdminSignupRequest(
                "admin@mapo.go.kr",
                "홍길동",
                organization,
                rank,
                "Password!123",
                "Password!123"
        );
    }
}
