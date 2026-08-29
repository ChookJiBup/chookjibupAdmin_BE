package com.example.chookjibupadmin.festival.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalWithMapResult;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalLocationCommand;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalSeries;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.analysis.application.MapAnalysisQueueApplicationService;
import com.example.chookjibupadmin.map.command.application.dto.UploadedFestivalMap;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileSize;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalApplicationServiceWithMapTest {

    @InjectMocks
    private FestivalApplicationService service;

    @Mock
    private FestivalService festivalService;

    @Mock
    private FestivalSeriesService festivalSeriesService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private FestivalMapService festivalMapService;

    @Mock
    private MapAnalysisQueueApplicationService mapAnalysisQueueService;

    @Mock
    private FestivalLocationService festivalLocationService;

    @Mock
    private com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService
            visitorCountService;

    @Test
    @DisplayName("축제와 S3 저장 완료 배치도 메타데이터를 함께 저장한다")
    void success_CreateWithMap() {
        AdminAccount admin = admin();
        AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
        FestivalSeries series = FestivalSeries.create(FestivalName.of("테스트 축제"));
        ReflectionTestUtils.setField(series, "id", 10L);
        given(adminAccountService.getById(1L)).willReturn(admin);
        given(festivalSeriesService.findByNormalizedName("테스트축제"))
                .willReturn(Optional.of(series));
        given(festivalService.existsBySeriesIdAndYear(10L, 2026))
                .willReturn(false);
        given(festivalService.save(any(Festival.class))).willAnswer(invocation -> {
            Festival festival = invocation.getArgument(0);
            ReflectionTestUtils.setField(festival, "id", 20L);
            return festival;
        });
        given(festivalMapService.save(any(FestivalMap.class)))
                .willAnswer(invocation -> {
                    FestivalMap map = invocation.getArgument(0);
                    ReflectionTestUtils.setField(map, "id", 30L);
                    return map;
                });
        given(festivalLocationService.saveAll(any()))
                .willAnswer(
                        invocation -> {
                            List<FestivalLocation> locations = invocation.getArgument(0);
                            ReflectionTestUtils.setField(locations.getFirst(), "id", 40L);
                            return locations;
                        });
        UUID festivalPublicId = UUID.randomUUID();
        UploadedFestivalMap uploadedMap = uploadedMap();

        CreateFestivalWithMapResult result = service.createWithMap(
                command(),
                principal,
                festivalPublicId,
                uploadedMap
        );

        assertThat(result.festival().getPublicId()).isEqualTo(festivalPublicId);
        assertThat(result.festivalMap().getFestivalId()).isEqualTo(20L);
        assertThat(result.festivalMap().getPublicId())
                .isEqualTo(uploadedMap.publicId());
        assertThat(result.festivalMap().getLocationId()).isEqualTo(40L);
        then(adminFestivalRoleService).should().assignFestivalOwner(1L, 20L);
        then(festivalMapService).should().save(any(FestivalMap.class));
        then(mapAnalysisQueueService).should().enqueueInitial(result.festivalMap());
    }

    @Test
    @DisplayName("도면 포함 생성도 대표 좌표가 없으면 거절한다")
    void fail_CreateWithMap_PrimaryCoordinatesRequired() {
        given(adminAccountService.getById(1L)).willReturn(admin());
        given(festivalSeriesService.findByNormalizedName("테스트축제"))
                .willReturn(Optional.empty());
        given(festivalSeriesService.save(any(FestivalSeries.class)))
                .willAnswer(invocation -> {
                    FestivalSeries festivalSeries = invocation.getArgument(0);
                    ReflectionTestUtils.setField(festivalSeries, "id", 10L);
                    return festivalSeries;
                });
        given(festivalService.existsBySeriesIdAndYear(10L, 2026)).willReturn(false);

        CreateFestivalCommand withoutCoords = new CreateFestivalCommand(
                null,
                "테스트 축제",
                "테스트 축제 설명",
                "서울특별시 마포구 월드컵로 243",
                "월드컵공원",
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18),
                LocalTime.of(10, 0),
                LocalTime.of(21, 0)
        );

        assertThatThrownBy(() -> service.createWithMap(
                withoutCoords,
                new AdminPrincipal(1L, "owner@mapo.go.kr"),
                UUID.randomUUID(),
                uploadedMap()
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FESTIVAL_PRIMARY_LOCATION_COORDINATES_REQUIRED.getMessage());
    }

    private CreateFestivalCommand command() {
        return new CreateFestivalCommand(
                null,
                "테스트 축제",
                "테스트 축제 설명",
                List.of(new FestivalLocationCommand(
                        FestivalLocationType.MAIN_VENUE,
                        "월드컵공원",
                        "서울특별시 마포구 월드컵로 243",
                        null,
                        "월드컵공원",
                        null,
                        null,
                        new BigDecimal("37.5683000"),
                        new BigDecimal("126.8973000"),
                        true,
                        0
                )),
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18),
                LocalTime.of(10, 0),
                LocalTime.of(21, 0)
        );
    }

    private UploadedFestivalMap uploadedMap() {
        return new UploadedFestivalMap(
                UUID.randomUUID(),
                FestivalMapName.of("테스트 축제 배치도"),
                MapImageFileName.of("map.png"),
                MapImageObjectKey.of("original-key"),
                MapImageObjectKey.of("display-key"),
                MapImageObjectKey.of("analysis-key"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/jpeg"),
                MapImageFileSize.of(100),
                MapImageFileSize.of(90),
                MapImageFileSize.of(80),
                MapImageDimensions.of(800, 600),
                MapImageDimensions.of(800, 600),
                Sha256Checksum.of("a".repeat(64)),
                Sha256Checksum.of("b".repeat(64)),
                Sha256Checksum.of("c".repeat(64))
        );
    }

    private AdminAccount admin() {
        AdminAccount admin = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
        return admin;
    }
}
