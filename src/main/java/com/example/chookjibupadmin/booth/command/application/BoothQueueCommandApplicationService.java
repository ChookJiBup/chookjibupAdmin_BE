package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.booth.command.application.dto.BoothQueueResult;
import com.example.chookjibupadmin.booth.command.application.dto.UpdateBoothQueueCommand;
import com.example.chookjibupadmin.booth.command.application.dto.UpdateBoothQueueCommand.QueuePathPointCommand;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionEstimate;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionEstimator;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionModifierType;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.booth.command.domain.BoothQueue;
import com.example.chookjibupadmin.booth.command.domain.BoothQueueModifierType;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import com.example.chookjibupadmin.operator.command.application.FieldStaffAccountService;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부스 대기열 줄끝 좌표·거리를 수정한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BoothQueueCommandApplicationService {

    private static final BigDecimal KOREA_LAT_MIN = new BigDecimal("33.0");
    private static final BigDecimal KOREA_LAT_MAX = new BigDecimal("38.7");
    private static final BigDecimal KOREA_LNG_MIN = new BigDecimal("124.5");
    private static final BigDecimal KOREA_LNG_MAX = new BigDecimal("132.0");
    private static final BoothCongestionEstimator BOOTH_CONGESTION_ESTIMATOR =
            new BoothCongestionEstimator();

    private final FestivalOperationAccessService festivalOperationAccessService;
    private final BoothQueueService boothQueueService;
    private final BoothInfoService boothInfoService;
    private final BoothCongestionService boothCongestionService;
    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FieldStaffAccountService fieldStaffAccountService;

    public BoothQueueResult updateTail(
            UUID festivalPublicId,
            UUID queueId,
            UpdateBoothQueueCommand command,
            FestivalActorPrincipal principal
    ) {
        Long festivalId = festivalOperationAccessService.getAuthorizedFestivalId(
                festivalPublicId,
                principal
        );
        BoothQueue queue = boothQueueService.getByPublicId(queueId);
        if (!queue.belongsTo(festivalId)) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_NOT_FOUND);
        }
        BoothInfo booth = boothInfoService.getById(queue.getBoothId());
        validateTailCoordinates(command.tailLatitude(), command.tailLongitude());
        List<Map<String, BigDecimal>> path = toPathGeometry(command.path());

        CongestionModifier modifier = switch (principal) {
            case AdminPrincipal adminPrincipal -> updateAsAdmin(
                    festivalId,
                    queue,
                    command,
                    path,
                    adminPrincipal
            );
            case FieldStaffPrincipal staffPrincipal -> updateAsStaff(
                    festivalId,
                    queue,
                    command,
                    path,
                    staffPrincipal
            );
            default -> throw new CustomException(ErrorCode.UNAUTHORIZED);
        };
        BoothQueue savedQueue = boothQueueService.save(queue);
        recordEstimatedCongestion(booth.getId(), command.queueTailMeters(), modifier);
        return BoothQueueResult.from(savedQueue, booth.getBoothName(), modifier.name());
    }

    private CongestionModifier updateAsAdmin(
            Long festivalId,
            BoothQueue queue,
            UpdateBoothQueueCommand command,
            List<Map<String, BigDecimal>> path,
            AdminPrincipal principal
    ) {
        AdminAccount admin = adminAccountService.getById(principal.adminId());
        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(admin.getId(), festivalId);
        if (!role.canUpdateQueueTail()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        queue.updateTail(
                command.tailLatitude(),
                command.tailLongitude(),
                command.queueTailMeters(),
                path,
                BoothQueueModifierType.ADMIN,
                admin.getId(),
                null
        );
        return new CongestionModifier(
                BoothCongestionModifierType.ADMIN,
                admin.getId(),
                null,
                admin.getNameValue()
        );
    }

    private CongestionModifier updateAsStaff(
            Long festivalId,
            BoothQueue queue,
            UpdateBoothQueueCommand command,
            List<Map<String, BigDecimal>> path,
            FieldStaffPrincipal principal
    ) {
        if (!festivalId.equals(principal.festivalId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        queue.updateTail(
                command.tailLatitude(),
                command.tailLongitude(),
                command.queueTailMeters(),
                path,
                BoothQueueModifierType.STAFF,
                null,
                principal.fieldStaffId()
        );
        return new CongestionModifier(
                BoothCongestionModifierType.STAFF,
                null,
                principal.fieldStaffId(),
                fieldStaffAccountService.getById(principal.fieldStaffId()).getNameValue()
        );
    }

    private void recordEstimatedCongestion(
            Long boothId,
            Integer queueTailMeters,
            CongestionModifier modifier
    ) {
        Optional<BoothCongestionEstimate> estimate = BOOTH_CONGESTION_ESTIMATOR
                .estimate(queueTailMeters);
        if (estimate.isEmpty()) {
            return;
        }

        BoothCongestionEstimate value = estimate.get();
        Optional<BoothCongestion> latest = boothCongestionService
                .findLatestByBoothId(boothId);
        if (latest.filter(congestion -> congestion.getWaitMinutes() != null
                && congestion.getWaitMinutes() == value.waitMinutes()
                && congestion.getCongestionLevel() == value.congestionLevel())
                .isPresent()) {
            return;
        }

        BoothCongestion congestion = modifier.type() == BoothCongestionModifierType.ADMIN
                ? BoothCongestion.recordByAdmin(
                        boothId,
                        modifier.adminId(),
                        value.waitMinutes(),
                        value.congestionLevel()
                )
                : BoothCongestion.recordByStaff(
                        boothId,
                        modifier.staffId(),
                        value.waitMinutes(),
                        value.congestionLevel()
                );
        boothCongestionService.save(congestion);
    }

    private void validateTailCoordinates(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (lat.compareTo(KOREA_LAT_MIN) < 0
                || lat.compareTo(KOREA_LAT_MAX) > 0
                || lng.compareTo(KOREA_LNG_MIN) < 0
                || lng.compareTo(KOREA_LNG_MAX) > 0) {
            throw new CustomException(ErrorCode.FESTIVAL_LOCATION_COORDINATES_OUT_OF_KOREA);
        }
    }

    private List<Map<String, BigDecimal>> toPathGeometry(List<QueuePathPointCommand> path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return path.stream()
                .map(point -> {
                    if (point == null || point.lat() == null || point.lng() == null) {
                        throw new CustomException(ErrorCode.INVALID_REQUEST);
                    }
                    validateTailCoordinates(point.lat(), point.lng());
                    Map<String, BigDecimal> map = new LinkedHashMap<>();
                    map.put("lat", point.lat());
                    map.put("lng", point.lng());
                    return map;
                })
                .toList();
    }

    private record CongestionModifier(
            BoothCongestionModifierType type,
            Long adminId,
            Long staffId,
            String name
    ) {
    }
}
