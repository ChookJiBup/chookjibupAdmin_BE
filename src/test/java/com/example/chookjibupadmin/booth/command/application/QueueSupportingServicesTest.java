package com.example.chookjibupadmin.booth.command.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.chookjibupadmin.booth.command.domain.*;
import com.example.chookjibupadmin.booth.command.application.port.QueuePlanRecommendationPort;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.map.command.application.FestivalMapPresentationService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class QueueSupportingServicesTest {
    @Test void success_PlanService_DelegatesRepository() {
        var repository=mock(BoothQueuePlanRepository.class);
        var service=new BoothQueuePlanService(repository);
        var plan=BoothQueuePlan.create(1L,2L);
        when(repository.findByBoothId(2L)).thenReturn(Optional.of(plan));
        when(repository.save(plan)).thenReturn(plan);
        assertThat(service.findByBoothId(2L)).contains(plan);
        assertThat(service.save(plan)).isSameAs(plan);
    }
    @Test void success_MapReader_ParsesOnlyWgs84() {
        var reader=new BoothQueueMapReader(mock(RoadmapNodeService.class),new ObjectMapper(),
                mock(FestivalMapPresentationService.class),mock(BoothQueuePlanService.class),mock(BoothInfoService.class));
        assertThat(reader.points("{\"points\":[{\"lat\":37,\"lng\":127},{\"lat\":37.001,\"lng\":127}]}" )).hasSize(2);
        assertThat(reader.points("{\"points\":[{\"x\":0.5,\"y\":0.5}]}" )).isEmpty();
        assertThat(reader.points("invalid")).isEmpty();
    }
    @Test void fail_WriteAccess_CompletedFestival() {
        var festivalService=mock(FestivalService.class);
        var festival=mock(Festival.class);
        var id=UUID.randomUUID();
        when(festivalService.getByPublicId(id)).thenReturn(festival);
        when(festival.getEndDate()).thenReturn(LocalDate.of(2026,9,15));
        var access=new QueueWriteAccess(festivalService,Clock.fixed(Instant.parse("2026-09-16T00:00:00Z"),ZoneOffset.UTC));
        assertThatThrownBy(() -> access.requireOpen(id)).isInstanceOf(RuntimeException.class);
    }
    @Test void success_Recommendation_ValidatesOutsideModelCall() {
        var contextService=mock(QueueRecommendationContextService.class);
        var port=mock(QueuePlanRecommendationPort.class);
        var service=new QueuePlanRecommendationApplicationService(contextService,port);
        var id=UUID.randomUUID(); var actor=new AdminPrincipal(1L,"a@mapo.go.kr");
        var points=List.of(QueueGeometry.point(java.math.BigDecimal.valueOf(37),java.math.BigDecimal.valueOf(127)),
                QueueGeometry.point(java.math.BigDecimal.valueOf(37.0004),java.math.BigDecimal.valueOf(127)));
        var input=new QueuePlanRecommendationPort.Input(points.getFirst(),points,List.of(),40,1);
        var context=new QueueRecommendationContextService.Context(input,3L,0L);
        when(contextService.read(id,2L,40,1,actor)).thenReturn(context);
        when(port.recommend(input)).thenReturn(new QueuePlanRecommendationPort.Proposal(points,"추천"));
        assertThat(service.recommend(id,2L,40,1,actor).expectedNodeVersion()).isEqualTo(3);
        verify(contextService).validate(id,2L,context,points,actor);
    }
}
