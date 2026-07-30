package com.example.demoadmin.map.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.festival.command.application.FestivalService;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.domain.FestivalMapStatus;
import com.example.demoadmin.map.query.application.dto.FestivalMapObjectsView;
import com.example.demoadmin.map.query.application.dto.FestivalMapView;
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
class MapQueryApplicationServiceTest {

    @InjectMocks
    private MapQueryApplicationService applicationService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private MapQueryService mapQueryService;

    @Nested
    @DisplayName("getMapObjects")
    class GetMapObjects {

        @Test
        @DisplayName("축제 관리 권한이 있으면 지도 객체를 조회한다")
        void success_GetMapObjects() {
            // given
            UUID festivalId = UUID.randomUUID();
            UUID mapId = UUID.randomUUID();
            AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
            Festival festival = org.mockito.Mockito.mock(Festival.class);
            AdminFestivalRole role = org.mockito.Mockito.mock(
                    AdminFestivalRole.class
            );
            FestivalMapView map = new FestivalMapView(
                    mapId,
                    FestivalMapStatus.ANALYZED,
                    1745,
                    1577
            );
            given(festivalService.getByPublicId(festivalId)).willReturn(festival);
            given(festival.getId()).willReturn(10L);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    1L,
                    10L
            )).willReturn(role);
            given(role.canManageQueueDesign()).willReturn(true);
            given(mapQueryService.getMap(10L, mapId)).willReturn(map);
            given(mapQueryService.getObjects(10L, mapId)).willReturn(List.of());

            // when
            FestivalMapObjectsView result = applicationService.getMapObjects(
                    festivalId,
                    mapId,
                    principal
            );

            // then
            assertThat(result.map()).isEqualTo(map);
            assertThat(result.objects()).isEmpty();
        }

        @Test
        @DisplayName("인증 정보가 없으면 인증 필요 예외를 던진다")
        void fail_GetMapObjects_Unauthorized() {
            // given
            UUID festivalId = UUID.randomUUID();
            UUID mapId = UUID.randomUUID();

            // when & then
            assertThatThrownBy(() -> applicationService.getMapObjects(
                    festivalId,
                    mapId,
                    null
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
        }

        @Test
        @DisplayName("대기 동선 관리 권한이 없으면 접근 금지 예외를 던진다")
        void fail_GetMapObjects_Forbidden() {
            // given
            UUID festivalId = UUID.randomUUID();
            UUID mapId = UUID.randomUUID();
            AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
            Festival festival = org.mockito.Mockito.mock(Festival.class);
            AdminFestivalRole role = org.mockito.Mockito.mock(
                    AdminFestivalRole.class
            );
            given(festivalService.getByPublicId(festivalId)).willReturn(festival);
            given(festival.getId()).willReturn(10L);
            given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                    1L,
                    10L
            )).willReturn(role);
            given(role.canManageQueueDesign()).willReturn(false);

            // when & then
            assertThatThrownBy(() -> applicationService.getMapObjects(
                    festivalId,
                    mapId,
                    principal
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FORBIDDEN.getMessage());
        }
    }
}
