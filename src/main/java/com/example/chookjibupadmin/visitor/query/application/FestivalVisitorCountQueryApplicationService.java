package com.example.chookjibupadmin.visitor.query.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import com.example.chookjibupadmin.visitor.query.application.dto.FestivalVisitorCountView;
import com.example.chookjibupadmin.visitor.query.application.dto.FestivalVisitorDayView;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorDaySupport;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorInputSupport;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 방문 인원 입력 현황 조회 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalVisitorCountQueryApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalService festivalService;
    private final FestivalVisitorCountService visitorCountService;
    private final Clock clock;

    public FestivalVisitorCountView getVisitorCounts(
            UUID festivalPublicId,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalPublicId);
        adminFestivalRoleService.getByAdminAccountIdAndFestivalId(
                adminAccount.getId(),
                festival.getId()
        );

        List<FestivalDailyVisitorCount> dailyCounts = visitorCountService
                .findDailyByFestivalIdOrderByVisitDateAsc(festival.getId());
        Map<LocalDate, FestivalDailyVisitorCount> byDate = dailyCounts.stream()
                .collect(Collectors.toMap(
                        FestivalDailyVisitorCount::getVisitDate,
                        Function.identity()
                ));

        LocalDate today = LocalDate.now(clock);
        List<FestivalVisitorDayView> days = new ArrayList<>();
        int dayIndex = 1;
        int sum = 0;
        int filled = 0;
        LocalDate cursor = festival.getStartDate();
        LocalDate end = festival.getEndDate();
        while (!cursor.isAfter(end)) {
            FestivalDailyVisitorCount saved = byDate.get(cursor);
            boolean isSaved = saved != null;
            Integer count = isSaved ? saved.getVisitorCountValue() : null;
            if (isSaved) {
                filled++;
                sum += saved.getVisitorCountValue();
            }
            days.add(new FestivalVisitorDayView(
                    cursor,
                    dayIndex,
                    count,
                    cursor.isBefore(today),
                    isSaved
            ));
            dayIndex++;
            cursor = cursor.plusDays(1);
        }

        Optional<Integer> totalOverride = visitorCountService
                .findTotalByFestivalId(festival.getId())
                .map(FestivalTotalVisitorCount::getVisitorCountValue);
        var snapshot = FestivalVisitorInputSupport.resolve(
                festival,
                dailyCounts,
                totalOverride
        );
        int totalDayCount = FestivalVisitorDaySupport.totalDayCount(festival);
        boolean allDaysFilled = FestivalVisitorDaySupport.isAllDaysFilled(
                festival,
                dailyCounts
        );

        return new FestivalVisitorCountView(
                festival.getPublicId(),
                festival.getStartDate(),
                festival.getEndDate(),
                snapshot.inputMode(),
                days,
                filled,
                totalDayCount,
                allDaysFilled,
                sum,
                totalOverride.orElse(null),
                snapshot.totalSaved(),
                snapshot.effectiveVisitorCount(),
                snapshot.source(),
                snapshot.status(),
                snapshot.difference(),
                FestivalVisitorInputSupport.isReportReady(snapshot)
        );
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return adminAccountService.getById(principal.adminId());
    }
}
