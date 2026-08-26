package com.example.chookjibupadmin.map.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalStatus;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.festival.location.domain.FestivalLocation;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.CoordinateMapView;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.query.application.dto.MapCenterView;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이미지 없이 카카오맵 위경도 편집용 지도 버전을 준비한다. */
@Service
@RequiredArgsConstructor
public class FestivalMapCoordinateRegistrationApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService roleService;
    private final FestivalService festivalService;
    private final FestivalLocationService festivalLocationService;
    private final FestivalMapService mapService;
    private final FestivalRoadmapService roadmapService;

    @Transactional
    public CoordinateMapView ensureCoordinateMap(
            UUID festivalPublicId,
            String mapName,
            AdminPrincipal principal
    ) {
        AuthorizedEdit authorized = authorize(festivalPublicId, principal, true);
        return mapService.findCurrentByFestivalId(authorized.festivalId())
                .map(map -> toView(map, authorized.festivalId()))
                .orElseGet(() -> createCoordinateMap(
                        authorized.festivalId(),
                        authorized.adminId(),
                        mapName
                ));
    }

    @Transactional(readOnly = true)
    public CoordinateMapView getCurrentCoordinateMap(
            UUID festivalPublicId,
            AdminPrincipal principal
    ) {
        AuthorizedEdit authorized = authorize(festivalPublicId, principal, false);
        FestivalMap map = mapService.findCurrentByFestivalId(authorized.festivalId())
                .orElseThrow(() -> new CustomException(ErrorCode.FESTIVAL_MAP_NOT_FOUND));
        return toView(map, authorized.festivalId());
    }

    private CoordinateMapView createCoordinateMap(
            Long festivalId,
            Long adminId,
            String mapName
    ) {
        FestivalLocation primary = festivalLocationService.findAllByFestivalId(festivalId).stream()
                .filter(FestivalLocation::isPrimary)
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.FESTIVAL_MAP_LOCATION_REQUIRED));
        if (primary.getLatitude() == null || primary.getLongitude() == null) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_LOCATION_REQUIRED);
        }

        FestivalMap map = mapService.save(FestivalMap.coordinateOnly(
                festivalId,
                primary.getId(),
                FestivalMapName.of(mapName == null || mapName.isBlank()
                        ? "본행사 배치"
                        : mapName.trim()),
                adminId
        ));

        FestivalRoadmap roadmap = roadmapService.findByFestivalId(festivalId)
                .orElseGet(() -> FestivalRoadmap.createForCoordinateMap(
                        festivalId,
                        map.getId(),
                        adminId
                ));
        if (roadmap.getCurrentMapId() != map.getId()) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }
        roadmapService.save(roadmap);

        return toView(map, festivalId);
    }

    private CoordinateMapView toView(FestivalMap map, Long festivalId) {
        FestivalRoadmap roadmap = roadmapService.getByFestivalId(festivalId);
        MapCenterView center = resolveCenter(festivalId, map);
        return new CoordinateMapView(
                map.getPublicId(),
                map.getMapName().getValue(),
                roadmap.getEditRevision(),
                roadmap.getStatus().name(),
                center
        );
    }

    private MapCenterView resolveCenter(Long festivalId, FestivalMap map) {
        return festivalLocationService.findAllByFestivalId(festivalId).stream()
                .filter(FestivalLocation::isPrimary)
                .filter(location -> location.getLatitude() != null && location.getLongitude() != null)
                .findFirst()
                .map(location -> new MapCenterView(location.getLatitude(), location.getLongitude()))
                .orElseThrow(() -> new CustomException(ErrorCode.FESTIVAL_MAP_LOCATION_REQUIRED));
    }

    private AuthorizedEdit authorize(
            UUID festivalPublicId,
            AdminPrincipal principal,
            boolean requireDraft
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        AdminAccount admin = adminAccountService.getById(principal.adminId());
        if (!admin.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }
        Festival festival = festivalService.getByPublicId(festivalPublicId);
        if (requireDraft && festival.getStatus() != FestivalStatus.DRAFT) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }
        AdminFestivalRole role = roleService.getByAdminAccountIdAndFestivalId(
                admin.getId(),
                festival.getId()
        );
        if (!role.canModifyFestivalInfo()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return new AuthorizedEdit(festival.getId(), admin.getId());
    }

    private record AuthorizedEdit(Long festivalId, Long adminId) {
    }
}
