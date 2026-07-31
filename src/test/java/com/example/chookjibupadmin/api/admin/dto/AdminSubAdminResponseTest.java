package com.example.chookjibupadmin.api.admin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminView;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminSubAdminResponseTest {

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("운영자의 이름과 이메일을 포함한 직원 정보를 응답으로 변환한다")
        void success_From() {
            // given
            UUID adminId = UUID.randomUUID();
            AdminSubAdminView view = new AdminSubAdminView(
                    adminId,
                    "operator@korea.kr",
                    "김관리",
                    "마포구청 소속",
                    "관광정책과",
                    "주무관",
                    AdminStatus.ACTIVE
            );

            // when
            AdminSubAdminResponse response = AdminSubAdminResponse.from(view);

            // then
            assertThat(response.adminId()).isEqualTo(adminId);
            assertThat(response.name()).isEqualTo("김관리");
            assertThat(response.email()).isEqualTo("operator@korea.kr");
            assertThat(response.department()).isEqualTo("관광정책과");
            assertThat(response.rank()).isEqualTo("주무관");
        }
    }
}
