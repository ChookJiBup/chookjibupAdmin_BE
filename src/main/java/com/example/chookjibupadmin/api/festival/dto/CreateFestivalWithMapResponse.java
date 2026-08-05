package com.example.chookjibupadmin.api.festival.dto;

import com.example.chookjibupadmin.festival.command.application.dto.CreateFestivalWithMapResult;
import com.example.chookjibupadmin.festival.location.application.dto.FestivalLocationDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "축제 기본 정보와 배치도 이미지 생성 응답")
public record CreateFestivalWithMapResponse(
        CreateFestivalResponse festival,
        CreateFestivalMapResponse map
) {

    public static CreateFestivalWithMapResponse from(
            CreateFestivalWithMapResult result
    ) {
        return new CreateFestivalWithMapResponse(
                CreateFestivalResponse.from(result.festival()),
                CreateFestivalMapResponse.from(result.festivalMap(), result.analysisJob())
        );
    }

    public static CreateFestivalWithMapResponse from(
            CreateFestivalWithMapResult result,
            List<FestivalLocationDetail> locations
    ) {
        return new CreateFestivalWithMapResponse(
                CreateFestivalResponse.from(result.festival(), locations),
                CreateFestivalMapResponse.from(result.festivalMap(), result.analysisJob())
        );
    }
}
