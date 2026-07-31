package com.example.chookjibupadmin.admin.command.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminStatusTest {

    @Nested
    @DisplayName("canAuthenticate")
    class CanAuthenticate {

        @Test
        @DisplayName("활성 상태는 인증에 사용할 수 있다")
        void success_CanAuthenticate_Active() {
            // given
            AdminStatus status = AdminStatus.ACTIVE;

            // when
            boolean canAuthenticate = status.canAuthenticate();

            // then
            assertThat(canAuthenticate).isTrue();
        }

        @Test
        @DisplayName("정지 상태는 인증에 사용할 수 없다")
        void success_CanAuthenticate_Suspended() {
            // given
            AdminStatus status = AdminStatus.SUSPENDED;

            // when
            boolean canAuthenticate = status.canAuthenticate();

            // then
            assertThat(canAuthenticate).isFalse();
        }

        @Test
        @DisplayName("삭제 상태는 인증에 사용할 수 없다")
        void success_CanAuthenticate_Deleted() {
            // given
            AdminStatus status = AdminStatus.DELETED;

            // when
            boolean canAuthenticate = status.canAuthenticate();

            // then
            assertThat(canAuthenticate).isFalse();
        }
    }
}
