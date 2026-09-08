package com.example.chookjibupadmin.map.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.PublishedRoadmap;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.NodeReviewStatus;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 편집이 끝난 부스맵을 방문객 앱에 공개한다.
 *
 * <p>사용자 백엔드는 로드맵 상태가 PUBLISHED일 때만 부스·구역·부지 경계·팜플렛을 내려준다.
 * 이 명령이 없으면 관리자가 부스맵을 아무리 만들어도 방문객에게는 영영 보이지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class RoadmapPublishApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService roleService;
    private final FestivalService festivalService;
    private final FestivalMapService mapService;
    private final FestivalRoadmapService roadmapService;
    private final RoadmapNodeService nodeService;

    @Transactional
    public PublishedRoadmap publish(
            UUID festivalPublicId,
            UUID mapPublicId,
            AdminPrincipal principal
    ) {
        AuthorizedPublish authorized = authorize(festivalPublicId, principal);
        FestivalMap map = mapService.getByPublicId(mapPublicId);
        if (!map.belongsTo(authorized.festivalId())) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_NOT_FOUND);
        }
        map.validateReadable();

        FestivalRoadmap roadmap = roadmapService.getByFestivalIdForUpdate(
                authorized.festivalId()
        );
        if (!roadmap.getCurrentMapId().equals(map.getId())) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }

        // 분석 중이면 노드를 세어 볼 것도 없다. 먼저 걸러야 «부스 없음»이라는 엉뚱한 안내가 나가지 않는다.
        roadmap.ensurePublishable();

        int visibleBoothCount = visibleBoothCount(roadmap, map);
        if (visibleBoothCount == 0) {
            throw new CustomException(ErrorCode.ROADMAP_PUBLISH_NO_BOOTH);
        }

        roadmap.publish();
        return new PublishedRoadmap(
                roadmap.getStatus().name(),
                roadmap.getPublishedVersion(),
                visibleBoothCount
        );
    }

    /**
     * 방문객 지도에 실제로 그려질 부스 수를 센다.
     *
     * <p>사용자 백엔드는 검수가 끝난(CONFIRMED) 노드만 내려주고, 구역 목록도 부스 노드로만
     * 채운다. 그래서 AI가 찾아 두기만 하고 관리자가 확인하지 않은 노드는 공개해도 화면에
     * 아무것도 뜨지 않는다. 그런 상태로 공개하면 방문객은 «배치도 미공개» 대신 텅 빈 지도를
     * 보게 되는데, 이건 관리자가 의도한 결과일 수 없으므로 공개 자체를 막는다.</p>
     */
    private int visibleBoothCount(FestivalRoadmap roadmap, FestivalMap map) {
        return (int) nodeService.findAll(roadmap.getId(), map.getId()).stream()
                .filter(node -> node.getNodeType() == NodeType.BOOTH)
                .filter(node -> node.getReviewStatus() == NodeReviewStatus.CONFIRMED)
                .map(RoadmapNode::getPublicId)
                .distinct()
                .count();
    }

    /** 지도 저장·오버레이 등록과 같은 기준: 활성 계정 + 축제 정보 수정 권한. */
    private AuthorizedPublish authorize(
            UUID festivalPublicId,
            AdminPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        AdminAccount admin = adminAccountService.getById(principal.adminId());
        if (!admin.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }
        Festival festival = festivalService.getByPublicId(festivalPublicId);
        if (festival.getStatus() != FestivalStatus.DRAFT) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }
        AdminFestivalRole role = roleService.getByAdminAccountIdAndFestivalId(
                admin.getId(),
                festival.getId()
        );
        if (!role.canModifyFestivalInfo()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return new AuthorizedPublish(festival.getId());
    }

    private record AuthorizedPublish(Long festivalId) {
    }
}
