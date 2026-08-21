package com.example.chookjibupadmin.global.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.command.infrastructure.JwtTokenProvider;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.operator.command.application.FieldStaffAccountService;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import com.example.chookjibupadmin.operator.command.infrastructure.FieldStaffTokenProvider;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Import(SecurityAuthorizationIntegrationTest.SecurityProbeController.class)
@Transactional
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private FieldStaffAccountService fieldStaffAccountService;

    @Autowired
    private JwtTokenProvider adminTokenProvider;

    @Autowired
    private FieldStaffTokenProvider fieldStaffTokenProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void success_AdminAuthentication_AdminApi() throws Exception {
        mockMvc.perform(get("/api/security-probe/admin")
                        .with(authentication(adminAuthentication())))
                .andExpect(status().isOk());
    }

    @Test
    void fail_FieldStaffAuthentication_AdminApi_Forbidden() throws Exception {
        mockMvc.perform(get("/api/security-probe/admin")
                        .with(authentication(fieldStaffAuthentication())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void success_FieldStaffAuthentication_FieldStaffApi() throws Exception {
        mockMvc.perform(get("/api/field-staff/security-probe")
                        .with(authentication(fieldStaffAuthentication())))
                .andExpect(status().isOk());
    }

    @Test
    void fail_AdminAuthentication_FieldStaffApi_Forbidden() throws Exception {
        mockMvc.perform(get("/api/field-staff/security-probe")
                        .with(authentication(adminAuthentication())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void success_AdminAuthentication_SharedOperationApi() throws Exception {
        mockMvc.perform(get(
                        "/api/festivals/festival-id/operations/security-probe"
                ).with(authentication(adminAuthentication())))
                .andExpect(status().isOk());
    }

    @Test
    void success_FieldStaffAuthentication_SharedOperationApi() throws Exception {
        mockMvc.perform(get(
                        "/api/festivals/festival-id/operations/security-probe"
                ).with(authentication(fieldStaffAuthentication())))
                .andExpect(status().isOk());
    }

    @Test
    void success_AdminAuthentication_VisitorOperationApi() throws Exception {
        mockMvc.perform(get(
                        "/api/festivals/festival-id/operations/visitors/security-probe"
                ).with(authentication(adminAuthentication())))
                .andExpect(status().isOk());
    }

    @Test
    void fail_FieldStaffAuthentication_VisitorOperationApi_Forbidden()
            throws Exception {
        mockMvc.perform(get(
                        "/api/festivals/festival-id/operations/visitors/security-probe"
                ).with(authentication(fieldStaffAuthentication())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void fail_MissingAuthentication_ProtectedApi_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/security-probe/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void success_MissingAuthentication_InternalApi() throws Exception {
        mockMvc.perform(get("/internal/api/security-probe"))
                .andExpect(status().isOk());
    }

    @Test
    void success_RealAdminToken_AdminApi() throws Exception {
        AdminAccount account = adminAccountService.save(adminAccount());
        String token = adminTokenProvider.createAccessToken(account);

        mockMvc.perform(get("/api/security-probe/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get(
                        "/api/festivals/festival-id/operations/security-probe"
                ).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void success_RealFieldStaffToken_FieldStaffAndOperationApi() throws Exception {
        FieldStaffAccount account = fieldStaffAccountService.save(
                fieldStaffAccount()
        );
        String token = fieldStaffTokenProvider.createAccessToken(account);

        mockMvc.perform(get("/api/field-staff/security-probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get(
                        "/api/festivals/festival-id/operations/security-probe"
                ).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void fail_RealFieldStaffToken_AdminApi_Unauthorized() throws Exception {
        FieldStaffAccount account = fieldStaffAccountService.save(
                fieldStaffAccount()
        );
        String token = fieldStaffTokenProvider.createAccessToken(account);

        mockMvc.perform(get("/api/security-probe/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40103));
    }

    @Test
    void fail_OldFieldStaffToken_AfterPasswordChange_Unauthorized()
            throws Exception {
        FieldStaffAccount account = fieldStaffAccountService.save(
                fieldStaffAccount()
        );
        String token = fieldStaffTokenProvider.createAccessToken(account);
        account.changePassword(FieldStaffPasswordHash.of("new-password-hash"));

        mockMvc.perform(get("/api/field-staff/security-probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40103));
    }

    @Test
    void fail_FieldStaffToken_AfterDeactivation_Unauthorized() throws Exception {
        FieldStaffAccount account = fieldStaffAccountService.save(
                fieldStaffAccount()
        );
        String token = fieldStaffTokenProvider.createAccessToken(account);
        account.deactivate();

        mockMvc.perform(get("/api/field-staff/security-probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40103));
    }

    private Authentication adminAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new AdminPrincipal(1L, "admin@mapo.go.kr"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private Authentication fieldStaffAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new FieldStaffPrincipal(1L, 10L, "staff01", 0L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_FIELD_STAFF"))
        );
    }

    private AdminAccount adminAccount() {
        return AdminAccount.createAdmin(
                AdminEmail.of("security-admin@mapo.go.kr"),
                AdminName.of("보안관리자"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("password-hash")
        );
    }

    private FieldStaffAccount fieldStaffAccount() {
        return FieldStaffAccount.create(
                10L,
                FieldStaffLoginId.of("security01"),
                FieldStaffName.of("보안스태프"),
                FieldStaffPhoneNumber.of("010-1234-5678"),
                FieldStaffPasswordHash.of("password-hash"),
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 31, 23, 59)
        );
    }

    @RestController
    static class SecurityProbeController {

        @GetMapping("/api/security-probe/admin")
        String admin() {
            return "admin";
        }

        @GetMapping("/api/field-staff/security-probe")
        String fieldStaff() {
            return "field-staff";
        }

        @GetMapping("/api/festivals/{festivalId}/operations/security-probe")
        String operation() {
            return "operation";
        }

        @GetMapping(
                "/api/festivals/{festivalId}/operations/visitors/security-probe"
        )
        String visitorOperation() {
            return "visitor-operation";
        }

        @GetMapping("/internal/api/security-probe")
        String internal() {
            return "internal";
        }
    }
}
