package com.example.chookjibupadmin.festival.command.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalImageUrlRepairApplicationServiceTest {

    @Mock
    private FestivalService festivalService;

    @Mock
    private MapImageStoragePort imageStoragePort;

    @Test
    @DisplayName("기존 관리자 API 이미지 주소를 실제 S3 객체 주소로 보정한다")
    void success_Run_LegacyRepresentativeImageUrl() {
        UUID festivalId = UUID.randomUUID();
        Festival festival = org.mockito.Mockito.mock(Festival.class);
        given(festival.getPublicId()).willReturn(festivalId);
        given(festivalService.findAllWithLegacyRepresentativeImageUrl())
                .willReturn(List.of(festival));
        String objectKey = "public/festivals/%s/representative.png".formatted(festivalId);
        URI publicUrl = URI.create(
                "https://bucket.s3.ap-northeast-2.amazonaws.com/" + objectKey
        );
        given(imageStoragePort.createPublicUrl(objectKey)).willReturn(publicUrl);
        FestivalImageUrlRepairApplicationService service =
                new FestivalImageUrlRepairApplicationService(festivalService, imageStoragePort);
        service.run(org.mockito.Mockito.mock(org.springframework.boot.ApplicationArguments.class));

        then(festival).should().assignRepresentativeImage(publicUrl.toString());
    }
}
