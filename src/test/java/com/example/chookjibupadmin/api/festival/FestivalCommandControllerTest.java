package com.example.chookjibupadmin.api.festival;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.api.festival.dto.CreateFestivalRequest;
import com.example.chookjibupadmin.api.festival.dto.CreateFestivalWithMapResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalApplicationService;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalWithMapResult;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationQueryApplicationService;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.map.command.application.FestivalMapRegistrationApplicationService;
import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileSize;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FestivalCommandControllerTest {

    @InjectMocks
    private FestivalCommandController controller;

    @Mock
    private FestivalApplicationService festivalApplicationService;

    @Mock
    private FestivalMapRegistrationApplicationService registrationService;

    @Mock
    private FestivalLocationQueryApplicationService locationQueryService;

    @Test
    @DisplayName("multipart 축제 등록 요청의 이미지 파트를 프레임워크 독립 Command로 변환한다")
    void success_CreateWithMap() throws Exception {
        CreateFestivalRequest request = request();
        AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "festival-map.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        given(registrationService.create(any(), any(), any()))
                .willReturn(result());

        ApiResponse<CreateFestivalWithMapResponse> response =
                controller.createWithMap(request, image, principal);

        assertThat(response.data().festival().name()).isEqualTo(request.name());
        assertThat(response.data().map().storageStatus()).isEqualTo("UPLOADED");
        ArgumentCaptor<MapImageUploadCommand> captor =
                ArgumentCaptor.forClass(MapImageUploadCommand.class);
        then(registrationService).should().create(
                any(),
                captor.capture(),
                any()
        );
        assertThat(captor.getValue().originalFileName())
                .isEqualTo("festival-map.png");
        assertThat(captor.getValue().fileSize()).isEqualTo(3);
        assertThat(captor.getValue().inputStreamSupplier().open().readAllBytes())
                .containsExactly(1, 2, 3);
    }

    private CreateFestivalRequest request() {
        return new CreateFestivalRequest(
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
    }

    private CreateFestivalWithMapResult result() {
        Festival festival = Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("테스트 축제"),
                FestivalDescription.of("테스트 축제 설명"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 18)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
        FestivalMap festivalMap = FestivalMap.uploaded(
                UUID.randomUUID(),
                1L,
                FestivalMapName.of("테스트 축제 배치도"),
                MapImageFileName.of("festival-map.png"),
                MapImageObjectKey.of("original-key"),
                MapImageObjectKey.of("display-key"),
                MapImageObjectKey.of("analysis-key"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/png"),
                MapImageContentType.of("image/jpeg"),
                MapImageFileSize.of(3),
                MapImageFileSize.of(3),
                MapImageFileSize.of(2),
                MapImageDimensions.of(800, 600),
                MapImageDimensions.of(800, 600),
                Sha256Checksum.of("a".repeat(64)),
                Sha256Checksum.of("b".repeat(64)),
                Sha256Checksum.of("c".repeat(64)),
                1L
        );
        return new CreateFestivalWithMapResult(festival, festivalMap);
    }
}
