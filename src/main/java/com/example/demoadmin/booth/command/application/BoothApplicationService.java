package com.example.demoadmin.booth.command.application;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.auth.support.FestivalAccessPrincipal;
import com.example.demoadmin.booth.command.application.dto.BoothQueueTailResult;
import com.example.demoadmin.booth.command.application.dto.CreateBoothCommand;
import com.example.demoadmin.booth.command.application.dto.CreateBoothQueueLineCommand;
import com.example.demoadmin.booth.command.application.dto.UpdateBoothCommand;
import com.example.demoadmin.booth.command.application.dto.UpdateBoothQueueLineCommand;
import com.example.demoadmin.booth.command.application.dto.UpdateBoothQueueTailCommand;
import com.example.demoadmin.booth.command.domain.BoothOperatingStatus;
import com.example.demoadmin.booth.command.domain.BoothQueueLine;
import com.example.demoadmin.booth.command.domain.FestivalBooth;
import com.example.demoadmin.booth.command.domain.vo.BoothCategory;
import com.example.demoadmin.booth.command.domain.vo.BoothDescription;
import com.example.demoadmin.booth.command.domain.vo.BoothLineLabel;
import com.example.demoadmin.booth.command.domain.vo.BoothLocation;
import com.example.demoadmin.booth.command.domain.vo.BoothName;
import com.example.demoadmin.festival.command.application.FestivalService;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.operator.command.application.FieldStaffAccountService;
import com.example.demoadmin.operator.command.domain.FieldStaffAccount;
import com.example.demoadmin.operator.support.FieldStaffPrincipal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 부스와 대기 라인 쓰기 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BoothApplicationService {

    private final FestivalService festivalService;
    private final FestivalBoothService festivalBoothService;
    private final BoothQueueLineService boothQueueLineService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FieldStaffAccountService fieldStaffAccountService;
    private final Clock clock;

    public FestivalBooth createBooth(
            UUID festivalId,
            CreateBoothCommand command,
            AdminPrincipal principal
    ) {
        Festival festival = findFestival(festivalId);
        validateQueueDesignPermission(festival.getId(), principal);

        return festivalBoothService.save(FestivalBooth.create(
                festival.getId(),
                BoothName.of(command.name()),
                BoothCategory.of(command.category()),
                BoothLocation.of(command.location()),
                BoothDescription.of(command.description())
        ));
    }

    public FestivalBooth updateBooth(
            UUID festivalId,
            UUID boothId,
            UpdateBoothCommand command,
            AdminPrincipal principal
    ) {
        Festival festival = findFestival(festivalId);
        validateQueueDesignPermission(festival.getId(), principal);
        FestivalBooth booth = festivalBoothService.getByFestivalIdAndPublicIdForUpdate(
                festival.getId(),
                boothId
        );
        booth.updateBasicInfo(
                BoothName.of(command.name()),
                BoothCategory.of(command.category()),
                BoothLocation.of(command.location()),
                BoothDescription.of(command.description())
        );

        return booth;
    }

    public BoothQueueLine createQueueLine(
            UUID festivalId,
            UUID boothId,
            CreateBoothQueueLineCommand command,
            AdminPrincipal principal
    ) {
        FestivalBooth booth = findManagedBoothForQueueDesign(festivalId, boothId, principal);
        validateLineOrderNotDuplicated(booth.getId(), command.lineOrder());

        return boothQueueLineService.save(BoothQueueLine.create(
                booth.getId(),
                command.lineOrder(),
                BoothLineLabel.of(command.label()),
                command.expectedWaitingMinutes(),
                command.maxCapacity(),
                command.pathData(),
                command.entryPointData()
        ));
    }

    public BoothQueueLine updateQueueLine(
            UUID festivalId,
            UUID boothId,
            UUID lineId,
            UpdateBoothQueueLineCommand command,
            AdminPrincipal principal
    ) {
        FestivalBooth booth = findManagedBoothForQueueDesign(festivalId, boothId, principal);
        BoothQueueLine queueLine = boothQueueLineService.getByBoothIdAndPublicId(
                booth.getId(),
                lineId
        );
        validateLineOrderNotDuplicatedExceptSelf(
                booth.getId(),
                command.lineOrder(),
                queueLine.getId()
        );
        queueLine.update(
                command.lineOrder(),
                BoothLineLabel.of(command.label()),
                command.expectedWaitingMinutes(),
                command.maxCapacity(),
                command.pathData(),
                command.entryPointData()
        );
        booth.refreshCurrentQueueLine(queueLine);

        return queueLine;
    }

    public BoothQueueTailResult updateQueueTail(
            UUID festivalId,
            UUID boothId,
            UpdateBoothQueueTailCommand command,
            FestivalAccessPrincipal principal
    ) {
        Festival festival = findFestival(festivalId);
        validateQueueTailPermission(festival.getId(), principal);
        FestivalBooth booth = festivalBoothService.getByFestivalIdAndPublicIdForUpdate(
                festival.getId(),
                boothId
        );

        BoothOperatingStatus status = parseStatus(command.status());
        if (status == BoothOperatingStatus.CLOSED) {
            booth.close();
            return new BoothQueueTailResult(booth, null);
        }

        if (command.queueLineId() == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        BoothQueueLine queueLine = boothQueueLineService.getByBoothIdAndPublicId(
                booth.getId(),
                command.queueLineId()
        );
        if (status == BoothOperatingStatus.SATURATED) {
            booth.saturateWith(queueLine);
            return new BoothQueueTailResult(booth, queueLine);
        }

        booth.updateQueueTail(queueLine);
        return new BoothQueueTailResult(booth, queueLine);
    }

    private FestivalBooth findManagedBoothForQueueDesign(
            UUID festivalId,
            UUID boothId,
            AdminPrincipal principal
    ) {
        Festival festival = findFestival(festivalId);
        validateQueueDesignPermission(festival.getId(), principal);
        return festivalBoothService.getByFestivalIdAndPublicIdForUpdate(
                festival.getId(),
                boothId
        );
    }

    private Festival findFestival(UUID festivalId) {
        return festivalService.getByPublicId(festivalId);
    }

    private void validateQueueDesignPermission(
            Long festivalId,
            AdminPrincipal principal
    ) {
        AdminFestivalRole role = getRole(festivalId, principal);
        if (!role.canManageQueueDesign()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateQueueTailPermission(
            Long festivalId,
            FestivalAccessPrincipal principal
    ) {
        if (principal instanceof AdminPrincipal adminPrincipal) {
            AdminFestivalRole role = getRole(festivalId, adminPrincipal);
            if (!role.canUpdateQueueTail()) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
            return;
        }
        if (principal instanceof FieldStaffPrincipal fieldStaffPrincipal) {
            validateFieldStaff(festivalId, fieldStaffPrincipal);
            return;
        }

        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    private void validateFieldStaff(
            Long festivalId,
            FieldStaffPrincipal principal
    ) {
        if (!festivalId.equals(principal.festivalId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        FieldStaffAccount account = fieldStaffAccountService.getById(
                principal.fieldStaffId()
        );
        if (!festivalId.equals(account.getFestivalId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (!principal.loginId().equals(account.getLoginIdValue())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (account.isDeleted()) {
            throw new CustomException(ErrorCode.FIELD_STAFF_NOT_ACTIVE);
        }
        if (!account.isUsableAt(LocalDateTime.now(clock))) {
            throw new CustomException(ErrorCode.FIELD_STAFF_VALID_PERIOD_EXPIRED);
        }
    }

    private AdminFestivalRole getRole(
            Long festivalId,
            AdminPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                principal.adminId(),
                festivalId
        );
    }

    private void validateLineOrderNotDuplicated(
            Long boothId,
            int lineOrder
    ) {
        if (boothQueueLineService.existsByBoothIdAndLineOrder(boothId, lineOrder)) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_LINE_ORDER_DUPLICATED);
        }
    }

    private void validateLineOrderNotDuplicatedExceptSelf(
            Long boothId,
            int lineOrder,
            Long queueLineId
    ) {
        if (boothQueueLineService.existsByBoothIdAndLineOrderAndIdNot(
                boothId,
                lineOrder,
                queueLineId
        )) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_LINE_ORDER_DUPLICATED);
        }
    }

    private BoothOperatingStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return BoothOperatingStatus.OPERATING;
        }

        try {
            return BoothOperatingStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
