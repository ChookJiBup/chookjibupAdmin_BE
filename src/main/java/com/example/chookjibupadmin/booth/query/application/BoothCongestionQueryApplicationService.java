package com.example.chookjibupadmin.booth.query.application;

import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.booth.command.application.BoothCongestionService;
import com.example.chookjibupadmin.booth.command.application.BoothInfoService;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.booth.query.application.dto.FestivalCongestionView;
import com.example.chookjibupadmin.booth.query.application.dto.FestivalCongestionView.BoothCongestionItemView;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 단위 부스 혼잡 현황을 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoothCongestionQueryApplicationService {

    private final FestivalOperationAccessService festivalOperationAccessService;
    private final BoothInfoService boothInfoService;
    private final BoothCongestionService boothCongestionService;

    public FestivalCongestionView getCongestion(
            UUID festivalPublicId,
            FestivalActorPrincipal principal
    ) {
        Long festivalId = festivalOperationAccessService.getAuthorizedFestivalId(
                festivalPublicId,
                principal
        );
        List<BoothInfo> booths = boothInfoService.findAllByFestivalId(festivalId);
        Map<Long, BoothCongestion> latestByBooth = boothCongestionService
                .findLatestByBoothIds(booths.stream().map(BoothInfo::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        BoothCongestion::getBoothId,
                        Function.identity(),
                        (left, right) -> left
                ));

        List<BoothCongestionItemView> items = new ArrayList<>();
        int activeQueues = 0;
        int waitSum = 0;
        int waitCount = 0;
        LocalDateTime newest = null;

        for (BoothInfo booth : booths) {
            BoothCongestion congestion = latestByBooth.get(booth.getId());
            BoothCongestionLevel level = congestion == null ? null : congestion.getCongestionLevel();
            Integer waitMinutes = congestion == null ? null : congestion.getWaitMinutes();
            LocalDateTime updatedAt = congestion == null ? null : congestion.getCreatedAt();
            if (level != null && level != BoothCongestionLevel.LOW
                    && waitMinutes != null && waitMinutes > 0) {
                activeQueues++;
            }
            if (waitMinutes != null) {
                waitSum += waitMinutes;
                waitCount++;
            }
            if (updatedAt != null && (newest == null || updatedAt.isAfter(newest))) {
                newest = updatedAt;
            }
            items.add(new BoothCongestionItemView(
                    booth.getId(),
                    booth.getBoothName(),
                    level,
                    waitMinutes,
                    updatedAt
            ));
        }
        items.sort(Comparator.comparing(BoothCongestionItemView::boothId));

        return new FestivalCongestionView(
                festivalId,
                newest,
                booths.isEmpty() ? null : activeQueues,
                waitCount == 0 ? null : waitSum / waitCount,
                items
        );
    }
}
