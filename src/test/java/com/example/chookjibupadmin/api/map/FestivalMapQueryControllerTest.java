package com.example.chookjibupadmin.api.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.api.map.dto.FestivalMapReadUrlResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.map.command.application.FestivalMapReadUrlApplicationService;
import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;
import com.example.chookjibupadmin.map.query.application.FestivalMapAnalysisQueryApplicationService;
import com.example.chookjibupadmin.map.query.application.dto.MapAnalysisStatusView;
import com.example.chookjibupadmin.map.query.application.dto.MapCenterView;
import com.example.chookjibupadmin.map.query.application.dto.MapEditorView;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalMapQueryControllerTest {

    @InjectMocks
    private FestivalMapQueryController controller;

    @Mock
    private FestivalMapReadUrlApplicationService readUrlService;

    @Mock
    private FestivalMapAnalysisQueryApplicationService analysisQueryService;

    @Test
    @DisplayName("배치도 화면 표시용 조회 URL과 만료 시각을 응답한다")
    void success_CreateReadUrl() {
        UUID festivalId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        AdminPrincipal principal = new AdminPrincipal(1L, "owner@mapo.go.kr");
        URI url = URI.create("https://example.com/display.png");
        Instant expiresAt = Instant.now().plusSeconds(600);
        given(readUrlService.createReadUrl(festivalId, mapId, principal))
                .willReturn(new MapImageReadUrl(url, expiresAt));

        ApiResponse<FestivalMapReadUrlResponse> response =
                controller.createReadUrl(festivalId, mapId, principal);

        assertThat(response.data().readUrl()).isEqualTo(url);
        assertThat(response.data().expiresAt()).isEqualTo(expiresAt);
        then(readUrlService).should().createReadUrl(
                festivalId, mapId, principal
        );
    }

    @Test
    @DisplayName("도면 분석 작업 상태와 검증 집계를 응답한다")
    void success_ReadAnalysisStatus() {
        UUID festivalId=UUID.randomUUID(); UUID mapId=UUID.randomUUID(); UUID jobId=UUID.randomUUID();
        AdminPrincipal principal=new AdminPrincipal(1L,"owner@mapo.go.kr");
        given(analysisQueryService.status(festivalId,mapId,principal)).willReturn(
                new MapAnalysisStatusView(jobId,"COMPLETED",1,3,2,1,null,null,null,null));
        var response=controller.analysisStatus(festivalId,mapId,principal);
        assertThat(response.data().jobId()).isEqualTo(jobId);
        assertThat(response.data().acceptedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("편집 화면용 배경 이미지와 정규화 노드를 응답한다")
    void success_ReadEditor() {
        UUID festivalId=UUID.randomUUID(); UUID mapId=UUID.randomUUID();
        AdminPrincipal principal=new AdminPrincipal(1L,"owner@mapo.go.kr");
        MapAnalysisStatusView status=new MapAnalysisStatusView(UUID.randomUUID(),"COMPLETED",1,0,0,0,null,null,null,null);
        given(analysisQueryService.editor(festivalId,mapId,principal)).willReturn(new MapEditorView(mapId,
                URI.create("https://example.com/display.jpg"),Instant.now().plusSeconds(600),
                2000,1000,1,"REVIEW_REQUIRED",status,List.of(),
                new MapCenterView(new java.math.BigDecimal("37.5665"), new java.math.BigDecimal("126.9780"))));
        var response=controller.editor(festivalId,mapId,principal);
        assertThat(response.data().imageWidth()).isEqualTo(2000);
        assertThat(response.data().analysis().status()).isEqualTo("COMPLETED");
    }
}
