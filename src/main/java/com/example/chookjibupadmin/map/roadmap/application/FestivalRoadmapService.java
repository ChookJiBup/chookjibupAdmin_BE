package com.example.chookjibupadmin.map.roadmap.application;
import com.example.chookjibupadmin.global.response.*;
import com.example.chookjibupadmin.map.roadmap.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class FestivalRoadmapService {
    private final FestivalRoadmapRepository repository;
    @Transactional public FestivalRoadmap save(FestivalRoadmap roadmap){return repository.save(roadmap);}
    public FestivalRoadmap getByFestivalId(Long id){return repository.findByFestivalId(id)
            .orElseThrow(()->new CustomException(ErrorCode.FESTIVAL_ROADMAP_NOT_FOUND));}
}
