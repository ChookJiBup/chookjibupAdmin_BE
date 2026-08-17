package com.example.chookjibupadmin.address.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.address.query.application.dto.RoadAddressSearchResult;
import com.example.chookjibupadmin.address.query.application.port.RoadAddressSearchPort;
import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoadAddressSearchServiceTest {

    @InjectMocks
    private RoadAddressSearchService searchService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private RoadAddressSearchPort searchPort;

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("검색어 공백을 정규화하고 기본 페이징으로 검색한다")
        void success_Search_DefaultPage() {
            // given
            AdminAccount adminAccount = adminAccount();
            AdminPrincipal principal = principal(adminAccount);
            RoadAddressSearchResult expected =
                    new RoadAddressSearchResult(1, 10, 0, List.of());
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(adminAccount);
            given(searchPort.search("광주 비엔날레", 1, 10))
                    .willReturn(expected);

            // when
            RoadAddressSearchResult result = searchService.search(
                    " 광주   비엔날레 ",
                    null,
                    null,
                    principal
            );

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("검색어 2자와 페이지 크기 20은 허용한다")
        void success_Search_Boundary() {
            // given
            AdminAccount adminAccount = adminAccount();
            AdminPrincipal principal = principal(adminAccount);
            RoadAddressSearchResult expected =
                    new RoadAddressSearchResult(1, 20, 0, List.of());
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(adminAccount);
            given(searchPort.search("서울", 1, 20)).willReturn(expected);

            // when
            RoadAddressSearchResult result =
                    searchService.search("서울", 1, 20, principal);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("인증 주체가 없으면 인증 예외를 던진다")
        void fail_Search_Unauthorized_CustomException() {
            // given
            AdminPrincipal principal = null;

            // when & then
            assertThatThrownBy(() ->
                    searchService.search("서울", 1, 10, principal)
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
        }

        @Test
        @DisplayName("탈퇴한 관리자는 주소를 검색할 수 없다")
        void fail_Search_InactiveAdmin_CustomException() {
            // given
            AdminAccount adminAccount = adminAccount();
            adminAccount.withdraw();
            AdminPrincipal principal = principal(adminAccount);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(adminAccount);

            // when & then
            assertThatThrownBy(() ->
                    searchService.search("서울", 1, 10, principal)
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_ADMIN_INACTIVE.getMessage());
        }

        @Test
        @DisplayName("검색어가 2자 미만이면 요청 값 예외를 던진다")
        void fail_Search_ShortKeyword_CustomException() {
            assertInvalidKeyword("서");
        }

        @Test
        @DisplayName("검색어가 100자를 초과하면 요청 값 예외를 던진다")
        void fail_Search_LongKeyword_CustomException() {
            assertInvalidKeyword("가".repeat(101));
        }

        @Test
        @DisplayName("차단 특수문자가 포함되면 요청 값 예외를 던진다")
        void fail_Search_BlockedCharacter_CustomException() {
            assertInvalidKeyword("서울%도로");
        }

        @Test
        @DisplayName("SQL 예약어가 독립 단어로 포함되면 요청 값 예외를 던진다")
        void fail_Search_BlockedSqlKeyword_CustomException() {
            assertInvalidKeyword("서울 UNION 도로");
        }

        @Test
        @DisplayName("페이지가 1 미만이면 요청 값 예외를 던진다")
        void fail_Search_PageUnderMinimum_CustomException() {
            assertInvalidPageSize(0, 10);
        }

        @Test
        @DisplayName("페이지 크기가 1 미만이면 요청 값 예외를 던진다")
        void fail_Search_SizeUnderMinimum_CustomException() {
            assertInvalidPageSize(1, 0);
        }

        @Test
        @DisplayName("페이지 크기가 20을 초과하면 요청 값 예외를 던진다")
        void fail_Search_SizeOverMaximum_CustomException() {
            assertInvalidPageSize(1, 21);
        }

        private void assertInvalidKeyword(String keyword) {
            AdminAccount adminAccount = adminAccount();
            AdminPrincipal principal = principal(adminAccount);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(adminAccount);

            assertThatThrownBy(() ->
                    searchService.search(keyword, 1, 10, principal)
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        private void assertInvalidPageSize(Integer page, Integer size) {
            AdminAccount adminAccount = adminAccount();
            AdminPrincipal principal = principal(adminAccount);
            given(adminAccountService.getById(principal.adminId()))
                    .willReturn(adminAccount);

            assertThatThrownBy(() ->
                    searchService.search("서울", page, size, principal)
            )
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    private AdminAccount adminAccount() {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(adminAccount, "id", 1L);
        return adminAccount;
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(
                adminAccount.getId(),
                adminAccount.getEmailValue()
        );
    }
}
