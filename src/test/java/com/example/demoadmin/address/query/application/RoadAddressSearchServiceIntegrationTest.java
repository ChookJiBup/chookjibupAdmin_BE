package com.example.demoadmin.address.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.demoadmin.address.query.application.dto.RoadAddressSearchResult;
import com.example.demoadmin.address.query.application.port.RoadAddressSearchPort;
import com.example.demoadmin.admin.command.application.AdminAccountService;
import com.example.demoadmin.admin.command.domain.AdminAccount;
import com.example.demoadmin.admin.command.domain.vo.AdminEmail;
import com.example.demoadmin.admin.command.domain.vo.AdminName;
import com.example.demoadmin.admin.command.domain.vo.AdminOrganization;
import com.example.demoadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.demoadmin.auth.support.AdminPrincipal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RoadAddressSearchServiceIntegrationTest {

    @Autowired
    private RoadAddressSearchService searchService;

    @Autowired
    private AdminAccountService adminAccountService;

    @MockitoBean
    private RoadAddressSearchPort searchPort;

    @Test
    @DisplayName("저장된 활성 관리자가 외부 주소 검색 Port를 호출한다")
    void success_Search_PersistedAdmin() {
        // given
        AdminAccount adminAccount = adminAccountService.save(adminAccount());
        RoadAddressSearchResult expected =
                new RoadAddressSearchResult(1, 10, 0, List.of());
        given(searchPort.search("광주비엔날레", 1, 10))
                .willReturn(expected);

        // when
        RoadAddressSearchResult result = searchService.search(
                "광주비엔날레",
                1,
                10,
                new AdminPrincipal(
                        adminAccount.getId(),
                        adminAccount.getEmailValue()
                )
        );

        // then
        assertThat(result).isEqualTo(expected);
    }

    private AdminAccount adminAccount() {
        return AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminPasswordHash.of("encoded-password")
        );
    }
}
