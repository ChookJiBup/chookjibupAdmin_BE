package com.example.chookjibupadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminDepartment;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.FestivalMapDeletionTarget;
import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
import com.example.chookjibupadmin.map.command.application.dto.PreparedMapImage;
import com.example.chookjibupadmin.map.command.application.port.MapImagePreparationPort;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileSize;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import java.nio.file.Files;
import java.nio.file.Path;
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
class FestivalMapManagementApplicationServiceTest {

    @InjectMocks
    private FestivalMapManagementApplicationService service;

    @Mock private AdminAccountService adminAccountService;
    @Mock private AdminFestivalRoleService adminFestivalRoleService;
    @Mock private FestivalService festivalService;
    @Mock private FestivalMapService festivalMapService;
    @Mock private FestivalMapLifecycleApplicationService lifecycleService;
    @Mock private MapImagePreparationPort imagePreparationPort;
    @Mock private MapImageStoragePort imageStoragePort;

    private final UUID festivalPublicId = UUID.randomUUID();
    private final UUID currentMapId = UUID.randomUUID();
    private final AdminPrincipal principal = new AdminPrincipal(
            1L, "owner@mapo.go.kr"
    );

    @BeforeEach
    void setUp() {
        AdminAccount admin = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청"),
                AdminDepartment.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        Festival festival = festival();
        ReflectionTestUtils.setField(festival, "id", 20L);
        given(adminAccountService.getById(1L)).willReturn(admin);
        given(festivalService.getByPublicId(festivalPublicId)).willReturn(festival);
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(1L, 20L))
                .willReturn(AdminFestivalRole.createFestivalOwner(1L, 20L));
    }

    @Test
    @DisplayName("새 이미지 저장 후 기존 배치도를 교체한다")
    void success_Replace() throws Exception {
        given(festivalMapService.getByPublicId(currentMapId))
                .willReturn(festivalMap());
        given(imagePreparationPort.prepare(any())).willReturn(prepared());
        given(lifecycleService.replace(any(), any(), any()))
                .willAnswer(invocation -> invocation.getArgument(2));

        FestivalMap replaced = service.replace(
                festivalPublicId,
                currentMapId,
                "새 배치도",
                imageCommand(),
                principal
        );

        assertThat(replaced.getMapName().getValue()).isEqualTo("새 배치도");
        then(imageStoragePort).should(times(3)).upload(any());
        then(lifecycleService).should().replace(
                org.mockito.ArgumentMatchers.eq(currentMapId),
                org.mockito.ArgumentMatchers.eq(20L),
                any(FestivalMap.class)
        );
    }

    @Test
    @DisplayName("DB 교체 실패 시 새 original, display, analysis 객체를 보상 삭제한다")
    void fail_Replace_Database_CompensateUploads() throws Exception {
        given(festivalMapService.getByPublicId(currentMapId))
                .willReturn(festivalMap());
        given(imagePreparationPort.prepare(any())).willReturn(prepared());
        given(lifecycleService.replace(any(), any(), any()))
                .willThrow(new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS));

        assertThatThrownBy(() -> service.replace(
                festivalPublicId, currentMapId, null, imageCommand(), principal
        )).isInstanceOf(CustomException.class);

        then(imageStoragePort).should(times(3)).delete(any());
    }

    @Test
    @DisplayName("삭제 대상 객체를 모두 지운 뒤 삭제 완료 상태를 확정한다")
    void success_Delete() {
        given(lifecycleService.beginDeletion(currentMapId, 20L))
                .willReturn(new FestivalMapDeletionTarget(
                        "original-key", "display-key", "analysis-key"
                ));

        service.delete(festivalPublicId, currentMapId, principal);

        then(imageStoragePort).should().delete("original-key");
        then(imageStoragePort).should().delete("display-key");
        then(imageStoragePort).should().delete("analysis-key");
        then(lifecycleService).should().completeDeletion(currentMapId, 20L);
    }

    @Test
    @DisplayName("S3 삭제 실패 시 삭제 완료 상태를 확정하지 않는다")
    void fail_Delete_Storage_KeepDeleting() {
        given(lifecycleService.beginDeletion(currentMapId, 20L))
                .willReturn(new FestivalMapDeletionTarget(
                        "original-key", "display-key", "analysis-key"
                ));
        doNothing()
                .doThrow(new CustomException(ErrorCode.FESTIVAL_MAP_DELETE_FAILED))
                .when(imageStoragePort)
                .delete(anyString());

        assertThatThrownBy(() -> service.delete(
                festivalPublicId, currentMapId, principal
        )).isInstanceOf(CustomException.class);

        then(lifecycleService).should(never())
                .completeDeletion(any(), any());
    }

    private PreparedMapImage prepared() throws Exception {
        Path original = Files.createTempFile("map-original-", ".png");
        Path display = Files.createTempFile("map-display-", ".png");
        Path analysis = Files.createTempFile("map-analysis-", ".jpg");
        Files.write(original, new byte[]{1});
        Files.write(display, new byte[]{2});
        Files.write(analysis, new byte[]{3});
        return new PreparedMapImage(
                "map.png", original, display, analysis,
                "image/png", "image/png", "image/jpeg",
                "png", "png", "jpg", 1, 1, 1,
                800, 600, 800, 600,
                "a".repeat(64), "b".repeat(64), "c".repeat(64)
        );
    }

    private MapImageUploadCommand imageCommand() {
        return new MapImageUploadCommand(
                "map.png", "image/png", 1,
                () -> new java.io.ByteArrayInputStream(new byte[]{1})
        );
    }

    private FestivalMap festivalMap() {
        return FestivalMap.uploaded(
                currentMapId, 20L, FestivalMapName.of("기존 배치도"),
                MapImageFileName.of("map.png"),
                MapImageObjectKey.of("original-key"),
                MapImageObjectKey.of("display-key"),
                MapImageObjectKey.of("analysis-key"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/jpeg"),
                MapImageFileSize.of(1), MapImageFileSize.of(1),
                MapImageFileSize.of(1),
                MapImageDimensions.of(800, 600),
                MapImageDimensions.of(800, 600),
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
