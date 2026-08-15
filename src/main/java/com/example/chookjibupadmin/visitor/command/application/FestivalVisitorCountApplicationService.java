package com.example.chookjibupadmin.visitor.command.application;

import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import com.example.chookjibupadmin.visitor.command.application.dto.FestivalDailyVisitorCountResult;
import com.example.chookjibupadmin.visitor.command.application.dto.FestivalTotalVisitorCountResult;
import com.example.chookjibupadmin.visitor.command.application.dto.UpdateVisitorCountCommand;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import java.time.LocalDate;
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

    private final FestivalOperationAccessService accessService;
    private final FestivalService festivalService;
    private final FestivalVisitorCountService visitorCountService;

    public FestivalDailyVisitorCountResult updateDailyVisitorCount(
            UUID festivalPublicId,
            LocalDate visitDate,
            UpdateVisitorCountCommand command,
            FestivalActorPrincipal principal
    ) {
        Long festivalId = accessService.getAuthorizedFestivalId(
                festivalPublicId,
                principal
        );
        Festival festival = festivalService.getById(festivalId);
        validateVisitDate(festival, visitDate);

        VisitorCount visitorCount = VisitorCount.of(command.visitorCount());
        FestivalDailyVisitorCount dailyVisitorCount = visitorCountService
                .findDailyByFestivalIdAndVisitDateForUpdate(
                        festivalId,
                        visitDate
                )
                .map(existing -> {
                    existing.changeVisitorCount(visitorCount);
                    return existing;
                })
                .orElseGet(() -> FestivalDailyVisitorCount.create(
                        festivalId,
                        visitDate,
                        visitorCount
                ));

        FestivalDailyVisitorCount saved =
                visitorCountService.saveDaily(dailyVisitorCount);

        return new FestivalDailyVisitorCountResult(
                festival.getPublicId(),
                saved.getVisitDate(),
                saved.getVisitorCountValue()
        );
    }

    public FestivalTotalVisitorCountResult updateTotalVisitorCount(
            UUID festivalPublicId,
            UpdateVisitorCountCommand command,
            FestivalActorPrincipal principal
    ) {
        Long festivalId = accessService.getAuthorizedFestivalId(
                festivalPublicId,
                principal
        );
        Festival festival = festivalService.getById(festivalId);
        VisitorCount visitorCount = VisitorCount.of(command.visitorCount());
        FestivalTotalVisitorCount totalVisitorCount = visitorCountService
                .findTotalByFestivalIdForUpdate(festivalId)
                .map(existing -> {
                    existing.changeVisitorCount(visitorCount);
                    return existing;
                })
                .orElseGet(() -> FestivalTotalVisitorCount.create(
                        festivalId,
                        visitorCount
                ));

        FestivalTotalVisitorCount saved =
                visitorCountService.saveTotal(totalVisitorCount);

        return new FestivalTotalVisitorCountResult(
                festival.getPublicId(),
                saved.getVisitorCountValue()
        );
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
    }
}
