package com.example.chookjibupadmin.festival.command.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;

import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalDeletionTarget;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalDeleteApplicationServiceTest {

    @InjectMocks
    private FestivalDeleteApplicationService service;

    @Mock
    private FestivalDeletionLifecycleService lifecycleService;

    @Mock
    private MapImageStoragePort mapImageStoragePort;

    private final UUID festivalId = UUID.randomUUID();
    private final AdminPrincipal principal = new AdminPrincipal(
            1L,
            "owner@mapo.go.kr"
    );

    @Test
    @DisplayName("배치도 파일을 모두 지운 뒤 축제 DB 삭제를 완료한다")
    void success_Delete() {
        given(lifecycleService.beginDeletion(festivalId, principal))
                .willReturn(new FestivalDeletionTarget(List.of(
                        "original-key",
                        "display-key",
                        "analysis-key"
                )));

        service.delete(festivalId, principal);

        then(mapImageStoragePort).should().delete("original-key");
        then(mapImageStoragePort).should().delete("display-key");
        then(mapImageStoragePort).should().delete("analysis-key");
        then(lifecycleService).should().completeDeletion(festivalId, principal);
    }

    @Test
    @DisplayName("배치도 파일 삭제가 실패하면 축제 DB를 삭제하지 않는다")
    void fail_Delete_Storage_KeepDatabase() {
        given(lifecycleService.beginDeletion(festivalId, principal))
                .willReturn(new FestivalDeletionTarget(List.of(
                        "original-key",
                        "display-key"
                )));
        doNothing()
                .doThrow(new CustomException(ErrorCode.FESTIVAL_MAP_DELETE_FAILED))
                .when(mapImageStoragePort)
                .delete(anyString());

        assertThatThrownBy(() -> service.delete(festivalId, principal))
                .isInstanceOf(CustomException.class);

        then(lifecycleService).should(never()).completeDeletion(any(), any());
    }
}
