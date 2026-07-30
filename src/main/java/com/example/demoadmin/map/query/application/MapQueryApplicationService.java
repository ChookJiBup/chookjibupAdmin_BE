package com.example.demoadmin.map.query.application;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.festival.command.application.FestivalService;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.query.application.dto.FestivalMapObjectsView;
import com.example.demoadmin.map.query.application.dto.FestivalMapView;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 권한의 배치도 객체 조회 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapQueryApplicationService {

    private final FestivalService festivalService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final MapQueryService mapQueryService;

    /**
     * 관리 권한을 검증한 뒤 배치도와 지도 객체 목록을 조회한다.
     */
    public FestivalMapObjectsView getMapObjects(
            UUID festivalId,
            UUID mapId,
            AdminPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Festival festival = festivalService.getByPublicId(festivalId);
        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(
                        principal.adminId(),
                        festival.getId()
                );
        if (!role.canManageQueueDesign()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        FestivalMapView map = mapQueryService.getMap(festival.getId(), mapId);
        return new FestivalMapObjectsView(
                map,
                mapQueryService.getObjects(festival.getId(), mapId)
        );
    }
}
