package com.example.chookjibupadmin.api.admin.dto;

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

class DeleteSubAdminsRequestTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Nested
    @DisplayName("adminIds validation")
    class AdminIdsValidation {

        @Test
        @DisplayName("최소 경계인 UUID 한 개를 허용한다")
        void success_AdminIds_MinBoundary() {
            // given
            DeleteSubAdminsRequest request =
                    new DeleteSubAdminsRequest(List.of(UUID.randomUUID()));

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("최대 경계인 UUID 100개를 허용한다")
        void success_AdminIds_MaxBoundary() {
            // given
            List<UUID> adminIds = IntStream.range(0, 100)
                    .mapToObj(index -> UUID.randomUUID())
                    .toList();
            DeleteSubAdminsRequest request = new DeleteSubAdminsRequest(adminIds);

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("빈 UUID 목록을 거절한다")
        void fail_AdminIds_Empty_ConstraintViolation() {
            // given
            DeleteSubAdminsRequest request = new DeleteSubAdminsRequest(List.of());

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("UUID가 100개를 초과하면 거절한다")
        void fail_AdminIds_OverMax_ConstraintViolation() {
            // given
            List<UUID> adminIds = IntStream.range(0, 101)
                    .mapToObj(index -> UUID.randomUUID())
                    .toList();
            DeleteSubAdminsRequest request = new DeleteSubAdminsRequest(adminIds);

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("null UUID가 포함되면 거절한다")
        void fail_AdminIds_NullElement_ConstraintViolation() {
            // given
            DeleteSubAdminsRequest request = new DeleteSubAdminsRequest(
                    Collections.singletonList(null)
            );

            // when
            var violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
        }
    }
}
