package com.example.chookjibupadmin.map.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;
import com.example.chookjibupadmin.map.command.application.port.MapImageStoragePort;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 축제 도면의 관리자 검수 화면용 S3 조회 URL을 발급한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalMapReadUrlApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalService festivalService;
    private final FestivalMapService festivalMapService;
    private final MapImageStoragePort imageStoragePort;

    public MapImageReadUrl createReadUrl(
            UUID festivalPublicId,
            UUID mapId,
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
        adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                admin.getId(),
                festival.getId()
        );
        FestivalMap festivalMap = festivalMapService.getByPublicId(mapId);
        if (!festivalMap.belongsTo(festival.getId())) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_NOT_FOUND);
        }
        festivalMap.validateReadable();
        return imageStoragePort.createReadUrl(
                festivalMap.getDisplayImageKey().getValue()
        );
    }
}
