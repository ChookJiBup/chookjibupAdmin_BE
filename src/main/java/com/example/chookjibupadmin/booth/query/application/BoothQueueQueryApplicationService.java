package com.example.chookjibupadmin.booth.query.application;

import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.booth.command.application.BoothInfoService;
import com.example.chookjibupadmin.booth.command.application.BoothQueueService;
import com.example.chookjibupadmin.booth.command.application.dto.BoothQueueResult;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.booth.command.domain.BoothQueue;
import com.example.chookjibupadmin.booth.query.application.dto.FestivalQueueListView;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
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
 * 승인 부스별 대기열을 조회하고, 없으면 빈 대기열을 생성한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BoothQueueQueryApplicationService {

    private final FestivalOperationAccessService festivalOperationAccessService;
    private final BoothInfoService boothInfoService;
    private final BoothQueueService boothQueueService;

    public FestivalQueueListView getQueues(
            UUID festivalPublicId,
            FestivalActorPrincipal principal
    ) {
        Long festivalId = festivalOperationAccessService.getAuthorizedFestivalId(
                festivalPublicId,
                principal
        );
        List<BoothInfo> booths = boothInfoService.findAllByFestivalId(festivalId);
        Map<Long, BoothQueue> byBooth = boothQueueService
                .findAllByBoothIdIn(booths.stream().map(BoothInfo::getId).toList())
                .stream()
                .collect(Collectors.toMap(BoothQueue::getBoothId, Function.identity()));

        List<BoothQueueResult> results = new ArrayList<>();
        for (BoothInfo booth : booths) {
            BoothQueue queue = byBooth.get(booth.getId());
            if (queue == null) {
                queue = boothQueueService.save(BoothQueue.createEmpty(festivalId, booth.getId()));
            }
            results.add(BoothQueueResult.from(queue, booth.getBoothName()));
        }
        results.sort(Comparator.comparing(BoothQueueResult::boothId));
        return new FestivalQueueListView(festivalId, results);
    }
}
