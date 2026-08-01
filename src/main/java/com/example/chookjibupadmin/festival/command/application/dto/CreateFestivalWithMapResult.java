package com.example.chookjibupadmin.festival.command.application.dto;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.map.command.domain.FestivalMap;

/**
 * 축제 기본 정보와 최초 배치도 생성 결과이다.
 */
public record CreateFestivalWithMapResult(
        Festival festival,
        FestivalMap festivalMap
) {
}
