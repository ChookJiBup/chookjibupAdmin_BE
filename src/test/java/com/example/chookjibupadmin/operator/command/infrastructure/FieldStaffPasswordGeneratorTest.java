package com.example.chookjibupadmin.operator.command.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FieldStaffPasswordGeneratorTest {

    private final FieldStaffPasswordGenerator generator =
            new FieldStaffPasswordGenerator();

    @Nested
    @DisplayName("generate")
    class Generate {

        @Test
        @DisplayName("12자리 임시 비밀번호를 생성한다")
        void success_Generate() {
            // given

            // when
            String password = generator.generate();

            // then
            assertThat(password).hasSize(12);
            assertThat(password).isNotBlank();
        }
    }
}
