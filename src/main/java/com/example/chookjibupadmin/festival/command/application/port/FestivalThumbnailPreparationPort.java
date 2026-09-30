package com.example.chookjibupadmin.festival.command.application.port;

import com.example.chookjibupadmin.festival.command.application.dto.FestivalThumbnailUploadCommand;
import com.example.chookjibupadmin.festival.command.application.dto.PreparedFestivalThumbnail;

/**
 * 축제 대표 이미지를 사용자 화면용 썸네일로 준비하는 계약이다.
 */
public interface FestivalThumbnailPreparationPort {

    PreparedFestivalThumbnail prepare(FestivalThumbnailUploadCommand command);
}
