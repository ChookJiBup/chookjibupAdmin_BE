package com.example.chookjibupadmin.map.command.application.dto;

import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileSize;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import java.util.UUID;

/**
 * S3 저장이 끝난 축제 도면 원본·표시본·AI 분석본 메타데이터를 전달한다.
 */
public record UploadedFestivalMap(
        UUID publicId,
        FestivalMapName mapName,
        MapImageFileName originalFileName,
        MapImageObjectKey originalImageKey,
        MapImageObjectKey displayImageKey,
        MapImageObjectKey analysisImageKey,
        MapImageContentType originalContentType,
        MapImageContentType displayContentType,
        MapImageContentType analysisContentType,
        MapImageFileSize originalFileSize,
        MapImageFileSize displayFileSize,
        MapImageFileSize analysisFileSize,
        MapImageDimensions displayImageDimensions,
        MapImageDimensions analysisImageDimensions,
        Sha256Checksum originalChecksumSha256,
        Sha256Checksum displayChecksumSha256,
        Sha256Checksum analysisChecksumSha256
) {
}
