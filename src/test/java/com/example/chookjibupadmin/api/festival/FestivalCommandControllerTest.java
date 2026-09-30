package com.example.chookjibupadmin.api.festival;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.api.festival.dto.CreateFestivalRequest;
import com.example.chookjibupadmin.api.festival.dto.CreateFestivalResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalApplicationService;
import com.example.chookjibupadmin.festival.command.application.FestivalDeleteApplicationService;
import com.example.chookjibupadmin.festival.command.application.FestivalImageRegistrationApplicationService;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalThumbnailUploadCommand;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationQueryApplicationService;
import com.example.chookjibupadmin.festival.support.ReviewQrUrlBuilder;
import com.example.chookjibupadmin.global.response.ApiResponse;
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
    private FestivalDeleteApplicationService festivalDeleteApplicationService;

    @Mock
    private FestivalImageRegistrationApplicationService imageRegistrationService;

    @Mock
    private FestivalLocationQueryApplicationService locationQueryService;

    @Mock
    private ReviewQrUrlBuilder reviewQrUrlBuilder;

    @Test
    @DisplayName("multipart 축제 등록 요청의 이미지 파트를 프레임워크 독립 Command로 변환한다")
    void success_CreateWithImage() throws Exception {
        CreateFestivalRequest request = request();
        AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "festival-thumbnail.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        Festival festival = festival();
        given(imageRegistrationService.create(any(), any(), any()))
                .willReturn(festival);
        given(reviewQrUrlBuilder.buildReviewUrl(any()))
                .willReturn("https://user.chookjibup.store/festivals/dummy/review?source=qr");

        ApiResponse<CreateFestivalResponse> response =
                controller.createWithImage(request, image, principal);

        assertThat(response.data().name()).isEqualTo(request.name());
        assertThat(response.data().reviewQrUrl())
                .isEqualTo("https://user.chookjibup.store/festivals/dummy/review?source=qr");
        ArgumentCaptor<FestivalThumbnailUploadCommand> captor =
                ArgumentCaptor.forClass(FestivalThumbnailUploadCommand.class);
        then(imageRegistrationService).should().create(
                any(),
                captor.capture(),
                any()
        );
        assertThat(captor.getValue().originalFileName())
                .isEqualTo("festival-thumbnail.png");
        assertThat(captor.getValue().fileSize()).isEqualTo(3);
        assertThat(captor.getValue().inputStreamSupplier().open().readAllBytes())
                .containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("축제 삭제 요청을 애플리케이션 서비스에 전달한다")
    void success_Delete() {
        UUID festivalId = UUID.randomUUID();
        AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");

        ApiResponse<Void> response = controller.delete(festivalId, principal);

        assertThat(response.code()).isEqualTo(22018);
        assertThat(response.data()).isNull();
        then(festivalDeleteApplicationService).should().delete(
                festivalId,
                principal
        );
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

    private Festival festival() {
        return Festival.create(
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
    }
}
