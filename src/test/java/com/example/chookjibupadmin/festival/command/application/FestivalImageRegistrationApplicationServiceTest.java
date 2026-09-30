package com.example.chookjibupadmin.festival.command.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalThumbnailUploadCommand;
import com.example.chookjibupadmin.festival.command.application.dto.PreparedFestivalThumbnail;
import com.example.chookjibupadmin.festival.command.application.port.FestivalThumbnailPreparationPort;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalImageRegistrationApplicationServiceTest {

    @InjectMocks
    private FestivalImageRegistrationApplicationService service;

    @Mock
    private FestivalThumbnailPreparationPort thumbnailPreparationPort;

    @Mock
    private MapImageStoragePort imageStoragePort;

    @Mock
    private FestivalApplicationService festivalApplicationService;

    @Mock
    private PreparedFestivalThumbnail preparedThumbnail;

    @Mock
    private Festival festival;

    @Test
    @DisplayName("축제 생성 이미지는 AI 분석 입력이 아닌 대표 썸네일로 저장한다")
    void success_Create_WithRepresentativeThumbnail() {
        FestivalThumbnailUploadCommand imageCommand =
                new FestivalThumbnailUploadCommand(
                        "festival.png",
                        "image/png",
                        3,
                        () -> new ByteArrayInputStream(new byte[]{1, 2, 3})
                );
        AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
        given(thumbnailPreparationPort.prepare(imageCommand))
                .willReturn(preparedThumbnail);
        given(preparedThumbnail.path()).willReturn(Path.of("thumbnail.jpg"));
        given(preparedThumbnail.fileSize()).willReturn(10L);
        given(preparedThumbnail.contentType()).willReturn("image/jpeg");
        given(preparedThumbnail.checksumSha256()).willReturn("checksum");
        given(festivalApplicationService.createWithImage(
                any(CreateFestivalCommand.class),
                any(AdminPrincipal.class),
                any(),
                any()
        )).willReturn(festival);

        service.create(
                org.mockito.Mockito.mock(CreateFestivalCommand.class),
                imageCommand,
                "https://admin-api.example.com/api/public/festivals",
                principal
        );

        then(thumbnailPreparationPort).should().prepare(imageCommand);
        then(imageStoragePort).should().upload(any(StoredMapImageFile.class));
        then(preparedThumbnail).should().close();
    }
}
