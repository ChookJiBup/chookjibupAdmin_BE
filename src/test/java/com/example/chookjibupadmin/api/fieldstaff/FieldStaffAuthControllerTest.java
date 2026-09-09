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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
            String loginId = newLoginId();

            MvcResult createResult = createFieldStaff(owner, created.festivalId(), loginId);
            String createdBody = createResult.getResponse().getContentAsString();
            String staffId = JsonPath.read(createdBody, "$.data.staffId");
            String temporaryPassword = JsonPath.read(createdBody, "$.data.temporaryPassword");

            mockMvc.perform(loginRequest(created.festivalId(), loginId, temporaryPassword))
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

        @Test
        @DisplayName("축제 시작 전 계정으로 로그인하면 언제부터 가능한지 알려 준다")
        void fail_Login_BeforeValidPeriod_TellsStartDate() throws Exception {
            AdminAccount owner = persistOwner();
            LocalDate startDate = LocalDate.now().plusMonths(2);
            CreatedFestival created = createFestival(
                    owner,
                    "스태프기간축제-" + UUID.randomUUID(),
                    startDate,
                    startDate.plusDays(2),
                    null
            );
            String loginId = newLoginId();

            MvcResult createResult = createFieldStaff(owner, created.festivalId(), loginId);
            String temporaryPassword = JsonPath.read(
                    createResult.getResponse().getContentAsString(),
                    "$.data.temporaryPassword"
            );
            LocalDate loginableFrom = startDate.minusDays(7);
            String expectedMessage = "%d년 %d월 %d일부터 로그인할 수 있습니다.".formatted(
                    loginableFrom.getYear(),
                    loginableFrom.getMonthValue(),
                    loginableFrom.getDayOfMonth()
            );

            mockMvc.perform(loginRequest(created.festivalId(), loginId, temporaryPassword))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(40303))
                    .andExpect(jsonPath("$.message").value(expectedMessage));
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("임시 비밀번호를 본인 비밀번호로 바꾸면 새 비밀번호로 로그인된다")
        void success_ChangePassword_ThenLoginWithNewPassword() throws Exception {
            AdminAccount owner = persistOwner();
            LocalDate today = LocalDate.now();
            CreatedFestival created = createFestival(
                    owner,
                    "스태프비번축제-" + UUID.randomUUID(),
                    today.minusDays(1),
                    today.plusDays(1),
                    null
            );
            String loginId = newLoginId();

            MvcResult createResult = createFieldStaff(owner, created.festivalId(), loginId);
            String temporaryPassword = JsonPath.read(
                    createResult.getResponse().getContentAsString(),
                    "$.data.temporaryPassword"
            );

            MvcResult loginResult = mockMvc.perform(
                            loginRequest(created.festivalId(), loginId, temporaryPassword))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.passwordChangeRequired").value(true))
                    .andReturn();
            String accessToken = JsonPath.read(
                    loginResult.getResponse().getContentAsString(),
                    "$.data.accessToken"
            );

            mockMvc.perform(post("/api/field-staff/auth/password")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "currentPassword": "%s",
                                      "newPassword": "NewPassword!123"
                                    }
                                    """.formatted(temporaryPassword)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.passwordChangeRequired").value(false));

            mockMvc.perform(
                            loginRequest(created.festivalId(), loginId, "NewPassword!123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.passwordChangeRequired").value(false));

            mockMvc.perform(
                            loginRequest(created.festivalId(), loginId, temporaryPassword))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("현재 비밀번호가 틀리면 401로 거절한다")
        void fail_ChangePassword_WrongCurrentPassword_Unauthorized() throws Exception {
            AdminAccount owner = persistOwner();
            LocalDate today = LocalDate.now();
            CreatedFestival created = createFestival(
                    owner,
                    "스태프비번오류축제-" + UUID.randomUUID(),
                    today.minusDays(1),
                    today.plusDays(1),
                    null
            );
            String loginId = newLoginId();

            MvcResult createResult = createFieldStaff(owner, created.festivalId(), loginId);
            String temporaryPassword = JsonPath.read(
                    createResult.getResponse().getContentAsString(),
                    "$.data.temporaryPassword"
            );
            MvcResult loginResult = mockMvc.perform(
                            loginRequest(created.festivalId(), loginId, temporaryPassword))
                    .andExpect(status().isOk())
                    .andReturn();
            String accessToken = JsonPath.read(
                    loginResult.getResponse().getContentAsString(),
                    "$.data.accessToken"
            );

            mockMvc.perform(post("/api/field-staff/auth/password")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "currentPassword": "wrong-password",
                                      "newPassword": "NewPassword!123"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(40105));
        }

        @Test
        @DisplayName("로그인하지 않으면 비밀번호를 바꿀 수 없다")
        void fail_ChangePassword_WithoutLogin_Unauthorized() throws Exception {
            mockMvc.perform(post("/api/field-staff/auth/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "currentPassword": "temporary",
                                      "newPassword": "NewPassword!123"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    private String newLoginId() {
        return "st" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private MvcResult createFieldStaff(
            AdminAccount owner,
            UUID festivalId,
            String loginId
    ) throws Exception {
        return mockMvc.perform(post(
                        "/api/festivals/{festivalId}/field-staff",
                        festivalId
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
    }

    private MockHttpServletRequestBuilder loginRequest(
            UUID festivalId,
            String loginId,
            String password
    ) {
        return post("/api/field-staff/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "festivalId": "%s",
                          "loginId": "%s",
                          "password": "%s"
                        }
                        """.formatted(festivalId, loginId, password));
    }
}
