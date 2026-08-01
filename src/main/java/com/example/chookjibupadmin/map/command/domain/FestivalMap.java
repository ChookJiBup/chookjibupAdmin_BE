package com.example.chookjibupadmin.map.command.domain;

import com.example.chookjibupadmin.common.domain.BaseTimeEntity;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제 배치도 원본과 화면 표시용 이미지의 저장 메타데이터를 관리한다.
 */
@Entity
@Getter
@Table(
        name = "festival_maps",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_festival_maps_public_id",
                        columnNames = "public_id"
                ),
                @UniqueConstraint(
                        name = "uk_festival_maps_source_image_key",
                        columnNames = "source_image_key"
                ),
                @UniqueConstraint(
                        name = "uk_festival_maps_display_image_key",
                        columnNames = "display_image_key"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalMap extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "festival_id", nullable = false, updatable = false)
    private Long festivalId;

    @Column(name = "map_name", nullable = false, length = 150)
    private String mapName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "source_image_key", nullable = false, length = 700)
    private String sourceImageKey;

    @Column(name = "display_image_key", nullable = false, length = 700)
    private String displayImageKey;

    @Column(name = "source_content_type", nullable = false, length = 50)
    private String sourceContentType;

    @Column(name = "display_content_type", nullable = false, length = 50)
    private String displayContentType;

    @Column(name = "source_file_size", nullable = false)
    private long sourceFileSize;

    @Column(name = "display_file_size", nullable = false)
    private long displayFileSize;

    @Column(name = "image_width", nullable = false)
    private int imageWidth;

    @Column(name = "image_height", nullable = false)
    private int imageHeight;

    @Column(name = "source_checksum_sha256", nullable = false, length = 64)
    private String sourceChecksumSha256;

    @Column(name = "display_checksum_sha256", nullable = false, length = 64)
    private String displayChecksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_status", nullable = false, length = 30)
    private FestivalMapStorageStatus storageStatus;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "created_by_admin_id", nullable = false, updatable = false)
    private Long createdByAdminId;

    @Version
    private Long version;

    private FestivalMap(
            UUID publicId,
            Long festivalId,
            String mapName,
            String originalFileName,
            String sourceImageKey,
            String displayImageKey,
            String sourceContentType,
            String displayContentType,
            long sourceFileSize,
            long displayFileSize,
            int imageWidth,
            int imageHeight,
            String sourceChecksumSha256,
            String displayChecksumSha256,
            Long createdByAdminId
    ) {
        this.publicId = publicId;
        this.festivalId = festivalId;
        this.mapName = mapName;
        this.originalFileName = originalFileName;
        this.sourceImageKey = sourceImageKey;
        this.displayImageKey = displayImageKey;
        this.sourceContentType = sourceContentType;
        this.displayContentType = displayContentType;
        this.sourceFileSize = sourceFileSize;
        this.displayFileSize = displayFileSize;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.sourceChecksumSha256 = sourceChecksumSha256;
        this.displayChecksumSha256 = displayChecksumSha256;
        this.storageStatus = FestivalMapStorageStatus.UPLOADED;
        this.current = true;
        this.createdByAdminId = createdByAdminId;
    }

    public static FestivalMap uploaded(
            UUID publicId,
            Long festivalId,
            String mapName,
            String originalFileName,
            String sourceImageKey,
            String displayImageKey,
            String sourceContentType,
            String displayContentType,
            long sourceFileSize,
            long displayFileSize,
            int imageWidth,
            int imageHeight,
            String sourceChecksumSha256,
            String displayChecksumSha256,
            Long createdByAdminId
    ) {
        if (publicId == null || festivalId == null || createdByAdminId == null
                || isBlank(mapName) || isBlank(originalFileName)
                || isBlank(sourceImageKey) || isBlank(displayImageKey)
                || isBlank(sourceContentType) || isBlank(displayContentType)
                || sourceFileSize <= 0 || displayFileSize <= 0
                || imageWidth <= 0 || imageHeight <= 0
                || isBlank(sourceChecksumSha256)
                || isBlank(displayChecksumSha256)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return new FestivalMap(
                publicId,
                festivalId,
                mapName,
                originalFileName,
                sourceImageKey,
                displayImageKey,
                sourceContentType,
                displayContentType,
                sourceFileSize,
                displayFileSize,
                imageWidth,
                imageHeight,
                sourceChecksumSha256,
                displayChecksumSha256,
                createdByAdminId
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
