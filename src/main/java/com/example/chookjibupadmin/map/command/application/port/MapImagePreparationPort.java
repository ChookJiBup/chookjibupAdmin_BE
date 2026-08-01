package com.example.chookjibupadmin.map.command.application.port;

import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
import com.example.chookjibupadmin.map.command.application.dto.PreparedMapImage;

/**
 * 업로드 원본을 검증하고 화면 표시용 이미지로 준비하는 계약이다.
 */
public interface MapImagePreparationPort {

    PreparedMapImage prepare(MapImageUploadCommand command);
}
