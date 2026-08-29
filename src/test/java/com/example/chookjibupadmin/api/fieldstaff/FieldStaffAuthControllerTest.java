package com.example.chookjibupadmin.api.fieldstaff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.support.AdminHttpIntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class FieldStaffAuthControllerTest extends AdminHttpIntegrationTestSupport {

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("축제를 만든 뒤 스태프를 생성하면 로그인과 비밀번호 재발급이 된다")
        void success_LoginAndReissue_AfterCreate() throws Exception {
            AdminAccount owner = persistOwner();
            LocalDate today = LocalDate.now();
            CreatedFestival created = createFestival(
                    owner,
                    "스태프검증축제-" + UUID.randomUUID(),
                    today.minusDays(1),
                    today.plusDays(1),
                    null
            );
            String loginId = "st" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

            MvcResult createResult = mockMvc.perform(post(
                            "/api/festivals/{festivalId}/field-staff",
                            created.festivalId()
                    )
                            .header("Authorization", bearer(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "loginId": "%s",
                                      "name": "김스태프",
                                      "phoneNumber": "010-1234-5678"
                                    }
                                    """.formatted(loginId)))
                    .andExpect(status().isCreated())
                    .andReturn();
            String createdBody = createResult.getResponse().getContentAsString();
            String staffId = JsonPath.read(createdBody, "$.data.staffId");
            String temporaryPassword = JsonPath.read(createdBody, "$.data.temporaryPassword");

            mockMvc.perform(post("/api/field-staff/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "festivalId": "%s",
                                      "loginId": "%s",
                                      "password": "%s"
                                    }
                                    """.formatted(
                                    created.festivalId(),
                                    loginId,
                                    temporaryPassword
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

            mockMvc.perform(post(
                            "/api/festivals/{festivalId}/field-staff/{staffId}/password/reissue",
                            created.festivalId(),
                            staffId
                    )
                            .header("Authorization", bearer(owner)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.temporaryPassword").isNotEmpty());
        }
    }
}
