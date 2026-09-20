package com.example.chookjibupadmin.api.admin.dto;

import com.example.chookjibupadmin.admin.query.application.dto.AdminReviewQrView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "축제 현장 리뷰 QR URL 조회 응답")
public record ReviewQrResponse(
        @Schema(description = "외부 노출용 축제 ID") UUID festivalId,
        @Schema(
                description = "QR코드로 인코딩할 리뷰 작성 URL(사용자 프런트 기준). "
                        + "프런트가 이 값을 그대로 QR 이미지로 그려서 보여준다.",
                example = "https://user.chookjibup.store/festivals/11111111-1111-1111-1111-111111111111/review?source=qr"
        )
        String reviewUrl
) {

    public static ReviewQrResponse from(AdminReviewQrView view) {
        return new ReviewQrResponse(view.festivalId(), view.reviewUrl());
    }
}