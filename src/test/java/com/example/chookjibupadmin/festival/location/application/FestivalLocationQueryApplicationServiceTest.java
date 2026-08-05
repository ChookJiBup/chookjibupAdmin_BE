package com.example.chookjibupadmin.festival.location.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.location.application.dto.FestivalLocationDetail;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationSourceType;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalLocationQueryApplicationServiceTest {

    @InjectMocks
    private FestivalLocationQueryApplicationService service;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService roleService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private FestivalLocationService locationService;

    @Nested
    @DisplayName("getLocations")
    class GetLocations {

        @Test
        @DisplayName("관리 권한을 확인하고 Entity가 아닌 조회 결과를 반환한다")
        void success_GetLocations_ReturnsProjection() {
            UUID festivalPublicId = UUID.randomUUID();
            AdminPrincipal principal = new AdminPrincipal(1L, "owner@example.com");
            AdminAccount admin = mock(AdminAccount.class);
            Festival festival = mock(Festival.class);
            FestivalLocation location = mock(FestivalLocation.class);
            UUID locationId = UUID.randomUUID();

            given(adminAccountService.getById(1L)).willReturn(admin);
            given(admin.getId()).willReturn(1L);
            given(admin.isActive()).willReturn(true);
            given(festivalService.getByPublicId(festivalPublicId)).willReturn(festival);
            given(festival.getId()).willReturn(10L);
            given(locationService.findAllByFestivalId(10L)).willReturn(List.of(location));
            given(location.getPublicId()).willReturn(locationId);
            given(location.getLocationType()).willReturn(FestivalLocationType.MAIN_VENUE);
            given(location.getLocationName()).willReturn("중앙광장");
            given(location.getSourceType()).willReturn(FestivalLocationSourceType.MANUAL);
            given(location.isPrimary()).willReturn(true);

            List<FestivalLocationDetail> result =
                    service.getLocations(festivalPublicId, principal);

            assertThat(result)
                    .singleElement()
                    .satisfies(detail -> {
                        assertThat(detail.locationId()).isEqualTo(locationId);
                        assertThat(detail.locationName()).isEqualTo("중앙광장");
                        assertThat(detail.primary()).isTrue();
                    });
            then(roleService)
                    .should()
                    .getByAdminAccountIdAndFestivalId(1L, 10L);
        }

        @Test
        @DisplayName("인증 정보가 없으면 조회를 거절한다")
        void fail_GetLocations_Unauthorized() {
            assertThatThrownBy(() -> service.getLocations(UUID.randomUUID(), null))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
        }
    }
}
