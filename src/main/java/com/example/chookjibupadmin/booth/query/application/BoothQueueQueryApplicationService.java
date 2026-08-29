package com.example.chookjibupadmin.booth.query.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.booth.command.application.BoothInfoService;
import com.example.chookjibupadmin.booth.command.application.BoothQueueService;
import com.example.chookjibupadmin.booth.command.application.dto.BoothQueueResult;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.booth.command.domain.BoothQueue;
import com.example.chookjibupadmin.booth.command.domain.BoothQueueModifierType;
import com.example.chookjibupadmin.booth.query.application.dto.FestivalQueueListView;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import com.example.chookjibupadmin.operator.command.application.FieldStaffAccountService;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final AdminAccountService adminAccountService;
    private final FieldStaffAccountService fieldStaffAccountService;

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

        Map<BoothInfo, BoothQueue> queueByBooth = new LinkedHashMap<>();
        for (BoothInfo booth : booths) {
            BoothQueue queue = byBooth.get(booth.getId());
            if (queue == null) {
                queue = boothQueueService.save(BoothQueue.createEmpty(festivalId, booth.getId()));
            }
            queueByBooth.put(booth, queue);
        }

        // 큐마다 이름을 따로 조회하면 N+1이 되므로 갱신자 이름은 한 번에 모아 조회한다.
        Collection<BoothQueue> queues = queueByBooth.values();
        Map<Long, String> adminNames = loadAdminNames(queues);
        Map<Long, String> staffNames = loadStaffNames(queues);

        List<BoothQueueResult> results = new ArrayList<>();
        queueByBooth.forEach((booth, queue) -> results.add(BoothQueueResult.from(
                queue,
                booth.getBoothName(),
                resolveModifierName(queue, adminNames, staffNames)
        )));
        results.sort(Comparator.comparing(BoothQueueResult::boothId));
        return new FestivalQueueListView(festivalId, results);
    }

    private Map<Long, String> loadAdminNames(Collection<BoothQueue> queues) {
        Set<Long> adminIds = queues.stream()
                .filter(queue -> queue.getModifierType() == BoothQueueModifierType.ADMIN)
                .map(BoothQueue::getModifierAdminId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (adminIds.isEmpty()) {
            return Map.of();
        }
        return adminAccountService.findAllById(adminIds).stream()
                .collect(Collectors.toMap(
                        AdminAccount::getId,
                        AdminAccount::getNameValue,
                        (a, b) -> a
                ));
    }

    private Map<Long, String> loadStaffNames(Collection<BoothQueue> queues) {
        Set<Long> staffIds = queues.stream()
                .filter(queue -> queue.getModifierType() == BoothQueueModifierType.STAFF)
                .map(BoothQueue::getModifierStaffId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (staffIds.isEmpty()) {
            return Map.of();
        }
        return fieldStaffAccountService.findAllById(staffIds).stream()
                .collect(Collectors.toMap(
                        FieldStaffAccount::getId,
                        FieldStaffAccount::getNameValue,
                        (a, b) -> a
                ));
    }

    private String resolveModifierName(
            BoothQueue queue,
            Map<Long, String> adminNames,
            Map<Long, String> staffNames
    ) {
        if (queue.getModifierType() == null) {
            return null;
        }
        return switch (queue.getModifierType()) {
            case ADMIN -> adminNames.get(queue.getModifierAdminId());
            case STAFF -> staffNames.get(queue.getModifierStaffId());
        };
    }
}
