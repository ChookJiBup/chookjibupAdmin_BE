package com.example.chookjibupadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileSize;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalMapReadUrlApplicationServiceTest {

    @InjectMocks
    private FestivalMapReadUrlApplicationService service;

    @Mock private AdminAccountService adminAccountService;
    @Mock private AdminFestivalRoleService adminFestivalRoleService;
    @Mock private FestivalService festivalService;
    @Mock private FestivalMapService festivalMapService;
    @Mock private MapImageStoragePort imageStoragePort;

    private final UUID festivalPublicId = UUID.randomUUID();
    private final UUID mapId = UUID.randomUUID();
    private final AdminPrincipal principal = new AdminPrincipal(
            1L, "owner@mapo.go.kr"
    );
    private Festival festival;
    private FestivalMap festivalMap;

    @BeforeEach
    void setUp() {
        AdminAccount admin = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        festival = festival();
        ReflectionTestUtils.setField(festival, "id", 20L);
        festivalMap = festivalMap(20L);
        given(adminAccountService.getById(1L)).willReturn(admin);
        given(festivalService.getByPublicId(festivalPublicId))
                .willReturn(festival);
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(1L, 20L))
                .willReturn(AdminFestivalRole.createFestivalOwner(1L, 20L));
    }

    @Test
    @DisplayName("권한이 있는 관리자에게 현재 배치도 display 이미지 URL을 발급한다")
    void success_CreateReadUrl() {
        MapImageReadUrl expected = new MapImageReadUrl(
                URI.create("https://example.com/display.png"),
                Instant.now().plusSeconds(600)
        );
        given(festivalMapService.getByPublicId(mapId)).willReturn(festivalMap);
        given(imageStoragePort.createReadUrl("display-key"))
                .willReturn(expected);

        MapImageReadUrl actual = service.createReadUrl(
                festivalPublicId, mapId, principal
        );

        assertThat(actual).isEqualTo(expected);
        then(imageStoragePort).should().createReadUrl("display-key");
    }

    @Test
    @DisplayName("다른 축제의 배치도에는 조회 URL을 발급하지 않는다")
    void fail_CreateReadUrl_OtherFestival_CustomException() {
        given(festivalMapService.getByPublicId(mapId))
                .willReturn(festivalMap(21L));

        assertThatThrownBy(() -> service.createReadUrl(
                festivalPublicId, mapId, principal
        )).isInstanceOf(CustomException.class);
    }

    private FestivalMap festivalMap(Long festivalId) {
        return FestivalMap.uploaded(
                mapId, festivalId, FestivalMapName.of("축제 배치도"),
                MapImageFileName.of("map.png"),
                MapImageObjectKey.of("original-key"),
                MapImageObjectKey.of("display-key"),
                MapImageObjectKey.of("analysis-key"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/jpeg"),
                MapImageFileSize.of(100L), MapImageFileSize.of(90L),
                MapImageFileSize.of(80L),
                MapImageDimensions.of(1200, 800),
                MapImageDimensions.of(1200, 800),
                Sha256Checksum.of("a".repeat(64)),
                Sha256Checksum.of("b".repeat(64)),
                Sha256Checksum.of("c".repeat(64)), 1L
        );
    }

    private Festival festival() {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("테스트 축제"),
                FestivalDescription.of("설명"),
                FestivalAddress.of("서울특별시 마포구"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 2)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(20, 0)
                )
        );
    }
}
