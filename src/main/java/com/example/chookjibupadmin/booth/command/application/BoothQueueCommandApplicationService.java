package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.booth.command.application.dto.BoothQueueResult;
import com.example.chookjibupadmin.booth.command.application.dto.UpdateBoothQueueCommand;

import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionEstimate;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;
import java.time.LocalDateTime;
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

    private final FestivalOperationAccessService festivalOperationAccessService;
    private final BoothQueueService boothQueueService;
    private final BoothInfoService boothInfoService;
    private final BoothCongestionService boothCongestionService;
    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FieldStaffAccountService fieldStaffAccountService;
    private final BoothQueuePlanService planService;
    private final BoothQueueMapReader mapReader;
    private final QueueWriteAccess writeAccess;
    private final Clock clock;

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
        writeAccess.requireOpen(festivalPublicId);
        BoothQueue queue = boothQueueService.getByPublicIdForUpdate(queueId);
        if (!queue.belongsTo(festivalId)) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_NOT_FOUND);
        }
        BoothInfo booth = boothInfoService.getByIdForUpdate(queue.getBoothId());
        queue.checkRevision(command.expectedRevision());
        validateTailCoordinates(command.tailLatitude(), command.tailLongitude());
        var plan = planService.findByBoothId(booth.getId()).orElse(null);
        var observation = QueueObservationResolver.resolve(queue, plan, mapReader.boothPoint(booth), command);
        List<Map<String, BigDecimal>> path = observation.path();
        command = new UpdateBoothQueueCommand(observation.lat(), observation.lng(), observation.meters(), null);

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
        queue.recordObservation(observation.estimate(), observation.refreshObservation() ? LocalDateTime.now(clock) : queue.getObservedAt(),
                observation.planRevision(), observation.method());
        BoothQueue savedQueue = boothQueueService.save(queue);
        recordEstimatedCongestion(booth.getId(), observation.estimate(), modifier);
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
            BoothCongestionEstimate value,
            CongestionModifier modifier
    ) {
        if (value == null) {
            return;
        }
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

    private record CongestionModifier(
            BoothCongestionModifierType type,
            Long adminId,
            Long staffId,
            String name
    ) {
    }
}
