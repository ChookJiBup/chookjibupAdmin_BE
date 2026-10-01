package com.example.chookjibupadmin.festival.command.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.support.FestivalPublicImageUrlBuilder;
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
    private FestivalPublicImageUrlBuilder publicImageUrlBuilder;

    @Test
    @DisplayName("S3 직접 주소를 서명 URL 중계 API 주소로 보정한다")
    void success_Run_LegacyRepresentativeImageUrl() {
        UUID festivalId = UUID.randomUUID();
        Festival festival = org.mockito.Mockito.mock(Festival.class);
        given(festival.getPublicId()).willReturn(festivalId);
        given(festivalService.findAllWithManagedRepresentativeImageUrl())
                .willReturn(List.of(festival));
        String publicUrl = "https://api.chookjibup.store/api/public/festivals/"
                + festivalId + "/image";
        given(publicImageUrlBuilder.build(festivalId)).willReturn(publicUrl);
        FestivalImageUrlRepairApplicationService service =
                new FestivalImageUrlRepairApplicationService(festivalService, publicImageUrlBuilder);
        service.run(org.mockito.Mockito.mock(org.springframework.boot.ApplicationArguments.class));

        then(festival).should().assignRepresentativeImage(publicUrl);
    }
}
