package com.example.chookjibupadmin.api.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.support.AdminHttpIntegrationTestSupport;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminManagedFestivalQueryControllerTest extends AdminHttpIntegrationTestSupport {

    @Nested
    @DisplayName("getManagedFestivals")
    class GetManagedFestivals {

        @Test
        @DisplayName("축제를 생성하면 내 관리 축제 목록이 200으로 축제명을 반환한다")
        void success_GetManagedFestivals_AfterCreate() throws Exception {
            AdminAccount owner = persistOwner();
            String name = "목록검증축제-" + UUID.randomUUID();
            CreatedFestival created = createFestival(
                    owner,
                    name,
                    LocalDate.of(2026, 8, 20),
                    LocalDate.of(2026, 8, 22),
                    "DAILY"
            );

            mockMvc.perform(get("/api/admin/me/managed-festivals")
                            .header("Authorization", bearer(owner)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].festivalId")
                            .value(created.festivalId().toString()))
                    .andExpect(jsonPath("$.data[0].festivalName").value(name));
        }

        @Test
        @DisplayName("내 관리 축제 단건 조회는 생성한 축제 UUID로 200을 반환한다")
        void success_GetManagedFestival_ById() throws Exception {
            AdminAccount owner = persistOwner();
            CreatedFestival created = createFestival(
                    owner,
                    "단건검증축제-" + UUID.randomUUID(),
                    LocalDate.of(2026, 8, 20),
                    LocalDate.of(2026, 8, 22),
                    null
            );

            mockMvc.perform(get("/api/admin/me/managed-festivals/{festivalId}",
                            created.festivalId())
                            .header("Authorization", bearer(owner)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.festivalId")
                            .value(created.festivalId().toString()));
        }
    }
}
