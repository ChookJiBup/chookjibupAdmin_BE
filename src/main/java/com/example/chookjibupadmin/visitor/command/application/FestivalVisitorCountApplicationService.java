package com.example.chookjibupadmin.visitor.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.report.command.application.FestivalReportGenerationApplicationService;
import com.example.chookjibupadmin.visitor.command.application.dto.FestivalDailyVisitorCountResult;
import com.example.chookjibupadmin.visitor.command.application.dto.FestivalTotalVisitorCountResult;
import com.example.chookjibupadmin.visitor.command.application.dto.UpdateVisitorCountCommand;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorDaySupport;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 방문 인원 수 입력 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FestivalVisitorCountApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalService festivalService;
    private final FestivalVisitorCountService visitorCountService;
    private final FestivalReportGenerationApplicationService reportGenerationService;
    private final Clock clock;

    public FestivalDailyVisitorCountResult updateDailyVisitorCount(
            UUID festivalPublicId,
            LocalDate visitDate,
            UpdateVisitorCountCommand command,
            AdminPrincipal principal
    ) {
        Festival festival = authorizeFestival(festivalPublicId, principal);
        validateVisitDate(festival, visitDate);

        VisitorCount visitorCount = VisitorCount.of(command.visitorCount());
        FestivalDailyVisitorCount dailyVisitorCount = visitorCountService
                .findDailyByFestivalIdAndVisitDateForUpdate(
                        festival.getId(),
                        visitDate
                )
                .map(existing -> {
                    existing.changeVisitorCount(visitorCount);
                    return existing;
                })
                .orElseGet(() -> FestivalDailyVisitorCount.create(
                        festival.getId(),
                        visitDate,
                        visitorCount
                ));

        FestivalDailyVisitorCount saved =
                visitorCountService.saveDaily(dailyVisitorCount);

        List<FestivalDailyVisitorCount> dailyCounts = visitorCountService
                .findDailyByFestivalIdOrderByVisitDateAsc(festival.getId());
        boolean allDaysFilled = FestivalVisitorDaySupport.isAllDaysFilled(
                festival,
                dailyCounts
        );
        boolean totalEntered = visitorCountService
                .findTotalByFestivalId(festival.getId())
                .isPresent();
        boolean reportReady = FestivalVisitorDaySupport.isVisitorInputReady(
                festival,
                dailyCounts,
                totalEntered
        );
        if (reportReady) {
            reportGenerationService.enqueueIfReady(festival.getId());
        }

        return new FestivalDailyVisitorCountResult(
                festival.getPublicId(),
                saved.getVisitDate(),
                saved.getVisitorCountValue(),
                allDaysFilled,
                reportReady
        );
    }

    public FestivalTotalVisitorCountResult updateTotalVisitorCount(
            UUID festivalPublicId,
            UpdateVisitorCountCommand command,
            AdminPrincipal principal
    ) {
        Festival festival = authorizeFestival(festivalPublicId, principal);
        VisitorCount visitorCount = VisitorCount.of(command.visitorCount());
        FestivalTotalVisitorCount totalVisitorCount = visitorCountService
                .findTotalByFestivalIdForUpdate(festival.getId())
                .map(existing -> {
                    existing.changeVisitorCount(visitorCount);
                    return existing;
                })
                .orElseGet(() -> FestivalTotalVisitorCount.create(
                        festival.getId(),
                        visitorCount
                ));

        FestivalTotalVisitorCount saved =
                visitorCountService.saveTotal(totalVisitorCount);
        reportGenerationService.enqueueIfReady(festival.getId());

        return new FestivalTotalVisitorCountResult(
                festival.getPublicId(),
                saved.getVisitorCountValue()
        );
    }

    private Festival authorizeFestival(
            UUID festivalPublicId,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalPublicId);
        adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                adminAccount.getId(),
                festival.getId()
        );
        return festival;
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return adminAccountService.getById(principal.adminId());
    }

    private void validateVisitDate(
            Festival festival,
            LocalDate visitDate
    ) {
        if (visitDate == null
                || visitDate.isBefore(festival.getStartDate())
                || visitDate.isAfter(festival.getEndDate())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        LocalDate today = LocalDate.now(clock);
        if (!visitDate.isBefore(today)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
