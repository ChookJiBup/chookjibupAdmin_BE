package com.example.chookjibupadmin.booth.command.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import com.example.chookjibupadmin.admin.command.application.*;
import com.example.chookjibupadmin.admin.command.domain.*;
import com.example.chookjibupadmin.admin.command.domain.vo.*;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.booth.command.application.dto.*;
import com.example.chookjibupadmin.booth.command.application.port.QueuePlanRecommendationPort;
import com.example.chookjibupadmin.booth.command.domain.*;
import com.example.chookjibupadmin.booth.query.application.BoothQueueQueryApplicationService;
import com.example.chookjibupadmin.festival.command.application.FestivalApplicationService;
import com.example.chookjibupadmin.festival.command.application.dto.*;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocationType;
import com.example.chookjibupadmin.global.response.*;
import com.example.chookjibupadmin.map.command.application.*;
import com.example.chookjibupadmin.map.command.application.dto.*;
import com.example.chookjibupadmin.map.command.domain.FestivalMapPresentation;
import com.example.chookjibupadmin.map.roadmap.application.*;
import com.example.chookjibupadmin.map.roadmap.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.*;

@SpringBootTest
@Transactional
class QueuePlanFlowIntegrationTest extends com.example.chookjibupadmin.support.AdminHttpIntegrationTestSupport {
    @Autowired AdminAccountService adminService;
    @Autowired AdminFestivalRoleService roleService;
    @Autowired FestivalApplicationService festivalService;
    @Autowired FestivalMapCoordinateRegistrationApplicationService coordinateService;
    @Autowired RoadmapDraftApplicationService draftService;
    @Autowired BoothApprovalApplicationService approvalService;
    @Autowired BoothInfoService boothService;
    @Autowired BoothQueueService queueService;
    @Autowired BoothQueueQueryApplicationService queueQuery;
    @Autowired BoothQueuePlanApplicationService planService;
    @Autowired BoothQueueCommandApplicationService observationService;
    @Autowired BoothCongestionCommandApplicationService congestionService;
    @Autowired QueuePlanRecommendationApplicationService recommendationService;
    @Autowired RoadmapNodeService nodeService;
    @Autowired FestivalRoadmapService roadmapService;
    @Autowired FestivalMapPresentationService presentationService;
    @Autowired ObjectMapper mapper;
    @Autowired EntityManager em;
    @Autowired Clock clock;
    @MockitoBean QueuePlanRecommendationPort recommendationPort;
    Festival festival;
    AdminPrincipal principal;
    BoothInfo booth;
    BoothQueue queue;
    RoadmapNode node;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("QUEUE_TEST_JDBC_URL");
        if (url != null) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.datasource.username", () -> "queue_test");
            registry.add("spring.datasource.password", () -> "");
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:queue_plan_flow;MODE=PostgreSQL;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS jsonb AS json\\;CREATE DOMAIN IF NOT EXISTS JSONB AS json");
        }
    }

    @BeforeEach void setUp() {
        var admin=adminService.save(AdminAccount.createAdmin(AdminEmail.of(UUID.randomUUID()+"@mapo.go.kr"),
                AdminName.of("대기줄 검증"),AdminOrganization.of("관광정책과"),AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")));
        principal=new AdminPrincipal(admin.getId(),admin.getEmailValue());
        festival=festivalService.create(new CreateFestivalCommand(null,"대기줄 검증","검증",
                List.of(new FestivalLocationCommand(FestivalLocationType.MAIN_VENUE,"본행사장","서울",null,
                        null,null,null,BigDecimal.valueOf(37),BigDecimal.valueOf(127),true,0)),
                LocalDate.now(clock),LocalDate.now(clock).plusDays(1),LocalTime.of(9,0),LocalTime.of(21,0)),principal);
        var map=coordinateService.ensureCoordinateMap(festival.getPublicId(),"배치",principal);
        draftService.save(festival.getPublicId(),map.mapId(),new SaveRoadmapDraftCommand(map.editRevision(),
                List.of(new RoadmapNodeChangeCommand(null,NodeType.BOOTH,"김밥부스",GeometryType.POINT,
                        mapper.valueToTree(Map.of("lat",37,"lng",127)),false,0))),principal);
        approvalService.approveAll(festival.getPublicId(),map.mapId(),principal);
        booth=boothService.findAllByFestivalId(festival.getId()).getFirst();
        queue=queueService.findByBoothId(booth.getId()).orElseThrow();
        node=nodeService.findAllById(List.of(booth.getRoadmapNodeId())).getFirst();
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) em.flush();
    }
    private Map<String,BigDecimal> p(double lat) { return QueueGeometry.point(BigDecimal.valueOf(lat),BigDecimal.valueOf(127)); }
    private SaveQueuePlanCommand planCommand(long revision) {
        return new SaveQueuePlanCommand(List.of(p(37),p(37.0004)),1,2,null,revision,node.getVersion());
    }

    @Test void success_Http_SavePlanThenTail_AlignedWithAdminFrontend() throws Exception {
        String planUrl="/api/festivals/"+festival.getPublicId()+"/operations/booths/"+booth.getId()+"/queue-plan";
        String queueUrl="/api/festivals/"+festival.getPublicId()+"/operations/queues/"+queue.getPublicId();
        String authorization=bearer(adminService.getById(principal.adminId()));
        String json=mapper.writeValueAsString(Map.of("path",List.of(p(37),p(37.0004)),
                "metersPerPerson",1,"servedPersonsPerMinute",2,"expectedRevision",0,
                "expectedNodeVersion",node.getVersion()));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(planUrl)
                .header("Authorization",authorization).contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(json))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.revision").value(1));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/festivals/"+festival.getPublicId()+"/operations/queues").header("Authorization",authorization))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.queues[0].waitMinutes").isEmpty());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(queueUrl)
                .header("Authorization",authorization).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"tailLatitude\":37.00018,\"tailLongitude\":127,\"expectedRevision\":0,\"planRevision\":1}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.waitMinutes").value(10))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.calculationMethod").value("PLAN"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.observationRevision").value(1));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(queueUrl)
                .header("Authorization",authorization).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"tailLatitude\":37.00018,\"tailLongitude\":127,\"expectedRevision\":0,\"planRevision\":1}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(40922));
    }

    @Test void fail_Http_PlanMissingAndInvalidFields_ReturnExpectedErrors() throws Exception {
        String url="/api/festivals/"+festival.getPublicId()+"/operations/booths/"+booth.getId()+"/queue-plan";
        String authorization=bearer(adminService.getById(principal.adminId()));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url).header("Authorization",authorization))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(40413));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                .header("Authorization",authorization).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"path\":[{\"lat\":37,\"lng\":127}],\"metersPerPerson\":1,\"servedPersonsPerMinute\":2,\"expectedRevision\":0}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test void success_Http_EditorIncludesNodeVersion() throws Exception {
        var map=roadmapService.getByFestivalId(festival.getId());
        // FE는 지도 편집 응답의 노드 version으로 사전 계획 저장을 검증한다.
        var mapEntity=em.find(com.example.chookjibupadmin.map.command.domain.FestivalMap.class,map.getCurrentMapId());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/festivals/"+festival.getPublicId()+"/maps/"+mapEntity.getPublicId()+"/editor")
                .header("Authorization",bearer(adminService.getById(principal.adminId()))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.nodes[0].version").value(node.getVersion()));
    }

    @Test void success_SavePlan_DoesNotCreateActualWait() {
        var result=planService.save(festival.getPublicId(),booth.getId(),planCommand(0),principal);
        em.flush(); em.clear();
        assertThat(result.revision()).isEqualTo(1);
        assertThat(planService.get(festival.getPublicId(),booth.getId(),principal).lengthMeters()).isBetween(44.0,45.0);
        assertThat(queueQuery.getQueues(festival.getPublicId(),principal).queues().getFirst().waitMinutes()).isNull();
    }
    @Test void success_UpdateTail_PathWithoutDistanceCreatesWait() {
        var path=List.of(new UpdateBoothQueueCommand.QueuePathPointCommand(BigDecimal.valueOf(37),BigDecimal.valueOf(127)),
                new UpdateBoothQueueCommand.QueuePathPointCommand(BigDecimal.valueOf(37.00018),BigDecimal.valueOf(127)));
        var result=observationService.updateTail(festival.getPublicId(),queue.getPublicId(),
                new UpdateBoothQueueCommand(BigDecimal.valueOf(37.00018),BigDecimal.valueOf(127),null,path,0L,null),principal);
        em.flush(); em.clear();
        assertThat(result.queueTailMeters()).isEqualTo(20);
        assertThat(result.waitMinutes()).isEqualTo(10);
        assertThat(result.observedAt()).isNotNull();
        assertThat(queueQuery.getQueues(festival.getPublicId(),principal).queues().getFirst().waitMinutes()).isEqualTo(10);
    }
    @Test void success_UpdateTail_PlanPartialAndRepeatedObservation() {
        planService.save(festival.getPublicId(),booth.getId(),planCommand(0),principal);
        var command=new UpdateBoothQueueCommand(BigDecimal.valueOf(37.00018),BigDecimal.valueOf(127),null,null,0L,1L);
        var first=observationService.updateTail(festival.getPublicId(),queue.getPublicId(),command,principal);
        var second=observationService.updateTail(festival.getPublicId(),queue.getPublicId(),
                new UpdateBoothQueueCommand(command.tailLatitude(),command.tailLongitude(),null,null,1L,1L),principal);
        assertThat(first.waitMinutes()).isEqualTo(10);
        assertThat(second.observationRevision()).isEqualTo(2);
        assertThat(second.observedAt()).isAfterOrEqualTo(first.observedAt());
    }
    @Test void fail_SavePlan_StaleRevision() {
        planService.save(festival.getPublicId(),booth.getId(),planCommand(0),principal);
        assertThatThrownBy(() -> planService.save(festival.getPublicId(),booth.getId(),planCommand(0),principal))
                .isInstanceOfSatisfying(CustomException.class,e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BOOTH_QUEUE_REVISION_CONFLICT));
    }
    @Test void success_RecordCongestion_ManualCorrectionUpdatesQueue() {
        observationService.updateTail(festival.getPublicId(),queue.getPublicId(),
                new UpdateBoothQueueCommand(BigDecimal.valueOf(37.00018),BigDecimal.valueOf(127),null,null),principal);
        congestionService.record(festival.getPublicId(),booth.getId(),
                new UpdateBoothCongestionCommand(45,BoothCongestionLevel.HIGH),principal);
        em.flush(); em.clear();
        var result=queueQuery.getQueues(festival.getPublicId(),principal).queues().getFirst();
        assertThat(result.waitMinutes()).isEqualTo(45);
        assertThat(result.calculationMethod()).isEqualTo("MANUAL");
        assertThat(result.path()).hasSize(2);
    }
    @Test void success_Recommend_ValidatedCandidateWithoutSaving() throws Exception {
        var presentation=FestivalMapPresentation.createEmpty(node.getMapId(),festival.getId());
        presentation.updateBoundary(mapper.writeValueAsString(Map.of("points",List.of(
                Map.of("lat",36.999,"lng",126.999),Map.of("lat",36.999,"lng",127.001),
                Map.of("lat",37.001,"lng",127.001),Map.of("lat",37.001,"lng",126.999)))));
        presentationService.save(presentation);
        given(recommendationPort.recommend(any())).willReturn(new QueuePlanRecommendationPort.Proposal(
                List.of(p(37),p(37.0004)),"통로를 피해 배치했습니다."));
        var result=recommendationService.recommend(festival.getPublicId(),booth.getId(),40,1,principal);
        assertThat(result.expectedRevision()).isZero();
        assertThat(result.path()).hasSize(2);
        assertThatThrownBy(() -> planService.get(festival.getPublicId(),booth.getId(),principal)).isInstanceOf(CustomException.class);
    }
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void success_UpdateTail_ConcurrencyRejectsStaleWriters() throws Exception {
        var executor=Executors.newFixedThreadPool(4);
        var start=new CountDownLatch(1);
        var ready=new CountDownLatch(4);
        var success=new AtomicInteger();
        var conflicts=new AtomicInteger();
        List<Future<?>> futures=new ArrayList<>();
        try {
            for(int i=0;i<4;i++) futures.add(executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    observationService.updateTail(festival.getPublicId(),queue.getPublicId(),
                            new UpdateBoothQueueCommand(BigDecimal.valueOf(37.00018),BigDecimal.valueOf(127),null,null,0L,null),principal);
                    success.incrementAndGet();
                } catch(CustomException e) {
                    if(e.getErrorCode()!=ErrorCode.BOOTH_QUEUE_REVISION_CONFLICT) throw e;
                    conflicts.incrementAndGet();
                } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            }));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); start.countDown();
            for(var future:futures) future.get(20,TimeUnit.SECONDS);
            assertThat(success.get()).isEqualTo(1); assertThat(conflicts.get()).isEqualTo(3);
            assertThat(queueService.findByBoothId(booth.getId()).orElseThrow().getObservationRevision()).isEqualTo(1);
        } finally { start.countDown(); executor.shutdownNow(); }
    }
}
