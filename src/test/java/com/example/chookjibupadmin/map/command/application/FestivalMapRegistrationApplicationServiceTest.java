package com.example.chookjibupadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalApplicationService;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalWithMapResult;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
import com.example.chookjibupadmin.map.command.application.dto.PreparedMapImage;
import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import com.example.chookjibupadmin.map.command.application.port.MapImagePreparationPort;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalMapRegistrationApplicationServiceTest {

    @InjectMocks
    private FestivalMapRegistrationApplicationService service;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private MapImagePreparationPort mapImagePreparationPort;

    @Mock
    private MapImageStoragePort mapImageStoragePort;

    @Mock
    private FestivalApplicationService festivalApplicationService;

    private AdminPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
        given(adminAccountService.getById(1L)).willReturn(admin());
    }

    @Test
    @DisplayName("original, display, analysis를 저장한 뒤 축제와 도면을 DB에 저장한다")
    void success_Create() throws Exception {
        PreparedMapImage prepared = prepared();
        given(mapImagePreparationPort.prepare(any())).willReturn(prepared);
        CreateFestivalWithMapResult expected = org.mockito.Mockito.mock(
                CreateFestivalWithMapResult.class
        );
        given(festivalApplicationService.createWithMap(
                any(), any(), any(), any()
        )).willReturn(expected);

        CreateFestivalWithMapResult result = service.create(
                command(),
                imageCommand(),
                principal
        );

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<StoredMapImageFile> captor =
                ArgumentCaptor.forClass(StoredMapImageFile.class);
        then(mapImageStoragePort).should(times(3)).upload(captor.capture());
        assertThat(captor.getAllValues().get(0).objectKey()).contains("/original/");
        assertThat(captor.getAllValues().get(1).objectKey()).contains("/display/");
        assertThat(captor.getAllValues().get(2).objectKey()).contains("/analysis/");
        then(festivalApplicationService).should().createWithMap(
                any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("display 업로드 실패 시 결과가 불명확한 display와 original을 보상 삭제한다")
    void fail_Create_DisplayUpload_CompensateOriginal() throws Exception {
        given(mapImagePreparationPort.prepare(any())).willReturn(prepared());
        doNothing()
                .doThrow(new CustomException(ErrorCode.FESTIVAL_MAP_UPLOAD_FAILED))
                .when(mapImageStoragePort)
                .upload(any());

        assertThatThrownBy(() -> service.create(
                command(), imageCommand(), principal
        )).isInstanceOf(CustomException.class);

        then(mapImageStoragePort).should(times(2)).delete(any());
        then(festivalApplicationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DB 저장 실패 시 original, display, analysis를 모두 보상 삭제한다")
    void fail_Create_Database_CompensateAllImages() throws Exception {
        given(mapImagePreparationPort.prepare(any())).willReturn(prepared());
        given(festivalApplicationService.createWithMap(
                any(), any(), any(), any()
        )).willThrow(new CustomException(ErrorCode.FESTIVAL_YEAR_ALREADY_EXISTS));

        assertThatThrownBy(() -> service.create(
                command(), imageCommand(), principal
        )).isInstanceOf(CustomException.class);

        then(mapImageStoragePort).should(times(3)).delete(any());
    }

    private PreparedMapImage prepared() throws Exception {
        Path original = Files.createTempFile("original-test-", ".png");
        Path display = Files.createTempFile("display-test-", ".png");
        Path analysis = Files.createTempFile("analysis-test-", ".jpg");
        Files.write(original, new byte[]{1, 2, 3});
        Files.write(display, new byte[]{4, 5, 6});
        Files.write(analysis, new byte[]{7, 8});
        return new PreparedMapImage(
                "map.png",
                original,
                display,
                analysis,
                "image/png",
                "image/png",
                "image/jpeg",
                "png",
                "png",
                "jpg",
                3,
                3,
                2,
                800,
                600,
                800,
                600,
                "a".repeat(64),
                "b".repeat(64),
                "c".repeat(64)
        );
    }

    private MapImageUploadCommand imageCommand() {
        return new MapImageUploadCommand(
                "map.png",
                "image/png",
                3,
                () -> new java.io.ByteArrayInputStream(new byte[]{1, 2, 3})
        );
    }

    private CreateFestivalCommand command() {
        return new CreateFestivalCommand(
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

    private AdminAccount admin() {
        return AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
    }
}
