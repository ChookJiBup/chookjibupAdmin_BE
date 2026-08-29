package com.example.chookjibupadmin.booth.query.application;

import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.booth.command.application.BoothCongestionService;
import com.example.chookjibupadmin.booth.command.application.BoothInfoService;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.booth.query.application.dto.FestivalOperationSuggestionView;
import com.example.chookjibupadmin.booth.query.application.dto.FestivalOperationSuggestionView.SuggestionItemView;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 혼잡 이력 기반 규칙형 운영 제안을 조회한다. OpenAI 연동 전 스텁이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalOperationSuggestionQueryApplicationService {

    private static final int LONG_WAIT_MINUTES = 30;

    private final FestivalOperationAccessService festivalOperationAccessService;
    private final BoothInfoService boothInfoService;
    private final BoothCongestionService boothCongestionService;

    public FestivalOperationSuggestionView getSuggestions(
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

        List<SuggestionItemView> suggestions = new ArrayList<>();
        for (BoothInfo booth : booths) {
            BoothCongestion congestion = latestByBooth.get(booth.getId());
            if (congestion == null) {
                continue;
            }
            if (congestion.getCongestionLevel() == BoothCongestionLevel.HIGH) {
                suggestions.add(new SuggestionItemView(
                        "rule-high-" + booth.getId(),
                        booth.getBoothName() + " 구역이 혼잡합니다",
                        "인접 부스·우회로로 방문객을 분산하는 안내를 권장합니다.",
                        List.of()
                ));
            } else if (congestion.getWaitMinutes() != null
                    && congestion.getWaitMinutes() >= LONG_WAIT_MINUTES) {
                suggestions.add(new SuggestionItemView(
                        "rule-wait-" + booth.getId(),
                        booth.getBoothName() + " 대기시간이 깁니다",
                        "줄 분리·안내 요원 배치로 대기열을 완화하는 것을 권장합니다.",
                        List.of()
                ));
            }
        }
        return new FestivalOperationSuggestionView(suggestions);
    }
}
