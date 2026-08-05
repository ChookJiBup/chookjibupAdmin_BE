package com.example.chookjibupadmin.festival.location.application;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.location.application.dto.FestivalLocationDetail;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalFestivalLocationQueryApplicationService {

    private final FestivalService festivalService;
    private final FestivalLocationService locationService;

    public List<FestivalLocationDetail> getLocations(UUID festivalId) {
        Festival festival = festivalService.getByPublicId(festivalId);
        return locationService.findAllByFestivalId(festival.getId()).stream()
                .map(FestivalLocationDetail::from)
                .toList();
    }
}
