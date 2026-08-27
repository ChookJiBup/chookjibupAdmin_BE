package com.example.chookjibupadmin.booth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.FestivalMapService;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BoothApprovalApplicationServiceTest {

    @InjectMocks
    private BoothApprovalApplicationService service;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private FestivalMapService festivalMapService;

    @Mock
    private RoadmapNodeService roadmapNodeService;

    @Mock
    private BoothInfoService boothInfoService;

    @Test
    @DisplayName("이미 같은 노드로 승인된 부스가 있으면 거절한다")
    void fail_Approve_AlreadyLinkedBooth() {
        Festival festival = festival(10L);
        FestivalMap map = map(10L, 5L);
        RoadmapNode node = boothNode(5L);
        ReflectionTestUtils.setField(node, "id", 77L);
        AdminAccount admin = admin();
        AdminPrincipal principal = new AdminPrincipal(admin.getId(), "hong@korea.kr");

        given(adminAccountService.getById(admin.getId())).willReturn(admin);
        given(festivalService.getByPublicId(festival.getPublicId())).willReturn(festival);
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(admin.getId(), 10L))
                .willReturn(AdminFestivalRole.createFestivalOwner(admin.getId(), 10L));
        given(festivalMapService.getByPublicId(map.getPublicId())).willReturn(map);
        given(roadmapNodeService.getByPublicIdAndMapIdForUpdate(node.getPublicId(), 5L))
                .willReturn(node);
        given(boothInfoService.findByFestivalIdAndRoadmapNodeId(10L, 77L))
                .willReturn(Optional.of(BoothInfo.create(10L, 77L, "김밥부스")));

        assertThatThrownBy(() -> service.approve(
                festival.getPublicId(),
                map.getPublicId(),
                node.getPublicId(),
                principal
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ROADMAP_NODE_ALREADY_APPROVED.getMessage());

        then(boothInfoService).should(never()).save(any());
    }

    @Test
    @DisplayName("부스 노드를 승인하면 booth_info를 만들고 노드에 연결한다")
    void success_Approve() {
        Festival festival = festival(10L);
        FestivalMap map = map(10L, 5L);
        RoadmapNode node = boothNode(5L);
        ReflectionTestUtils.setField(node, "id", 77L);
        AdminAccount admin = admin();
        AdminPrincipal principal = new AdminPrincipal(admin.getId(), "hong@korea.kr");
        BoothInfo saved = BoothInfo.create(10L, 77L, "김밥부스");
        ReflectionTestUtils.setField(saved, "id", 9L);

        given(adminAccountService.getById(admin.getId())).willReturn(admin);
        given(festivalService.getByPublicId(festival.getPublicId())).willReturn(festival);
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(admin.getId(), 10L))
                .willReturn(AdminFestivalRole.createFestivalOwner(admin.getId(), 10L));
        given(festivalMapService.getByPublicId(map.getPublicId())).willReturn(map);
        given(roadmapNodeService.getByPublicIdAndMapIdForUpdate(node.getPublicId(), 5L))
                .willReturn(node);
        given(boothInfoService.findByFestivalIdAndRoadmapNodeId(10L, 77L))
                .willReturn(Optional.empty());
        given(boothInfoService.save(any())).willReturn(saved);

        var result = service.approve(
                festival.getPublicId(),
                map.getPublicId(),
                node.getPublicId(),
                principal
        );

        assertThat(result.boothId()).isEqualTo(9L);
        assertThat(node.getRelatedBoothId()).isEqualTo(9L);
        then(roadmapNodeService).should().save(node);
    }

    private AdminAccount admin() {
        AdminAccount account = AdminAccount.createAdmin(
                AdminEmail.of("hong@korea.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("hash")
        );
        ReflectionTestUtils.setField(account, "id", 1L);
        return account;
    }

    private Festival festival(Long id) {
        Festival festival = Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("축제"),
                FestivalDescription.of("설명"),
                FestivalAddress.of("서울"),
                FestivalPeriod.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)),
                FestivalOperationTime.of(LocalTime.of(10, 0), LocalTime.of(20, 0))
        );
        ReflectionTestUtils.setField(festival, "id", id);
        return festival;
    }

    private FestivalMap map(Long festivalId, Long mapId) {
        FestivalMap map = FestivalMap.coordinateOnly(
                festivalId,
                1L,
                FestivalMapName.of("배치도"),
                1L
        );
        ReflectionTestUtils.setField(map, "id", mapId);
        return map;
    }

    private RoadmapNode boothNode(Long mapId) {
        return RoadmapNode.ai(
                20L,
                mapId,
                30L,
                NodeType.BOOTH,
                "김밥부스",
                GeometryType.RECTANGLE,
                "{\"x\":0.1,\"y\":0.2}",
                new BigDecimal("0.9000"),
                "김밥부스",
                0
        );
    }
}
