package com.example.chookjibupadmin.api.fieldstaff.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DeleteFieldStaffRequestTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Nested
    @DisplayName("staffIds validation")
    class StaffIdsValidation {

        @Test
        @DisplayName("최소 경계인 UUID 한 개를 허용한다")
        void success_StaffIds_MinBoundary() {
            // given
            DeleteFieldStaffRequest request =
                    new DeleteFieldStaffRequest(List.of(UUID.randomUUID()));

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("최대 경계인 UUID 100개를 허용한다")
        void success_StaffIds_MaxBoundary() {
            // given
            List<UUID> staffIds = IntStream.range(0, 100)
                    .mapToObj(index -> UUID.randomUUID())
                    .toList();
            DeleteFieldStaffRequest request = new DeleteFieldStaffRequest(staffIds);

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("빈 UUID 목록을 거절한다")
        void fail_StaffIds_Empty_ConstraintViolation() {
            // given
            DeleteFieldStaffRequest request = new DeleteFieldStaffRequest(List.of());

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("UUID가 100개를 초과하면 거절한다")
        void fail_StaffIds_OverMax_ConstraintViolation() {
            // given
            List<UUID> staffIds = IntStream.range(0, 101)
                    .mapToObj(index -> UUID.randomUUID())
                    .toList();
            DeleteFieldStaffRequest request = new DeleteFieldStaffRequest(staffIds);

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("null UUID가 포함되면 거절한다")
        void fail_StaffIds_NullElement_ConstraintViolation() {
            // given
            DeleteFieldStaffRequest request = new DeleteFieldStaffRequest(
                    Collections.singletonList(null)
            );

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }
    }
}
