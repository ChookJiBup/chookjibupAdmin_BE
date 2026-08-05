package com.example.chookjibupadmin.map.command.domain;

import com.example.chookjibupadmin.common.domain.BaseTimeEntity;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.domain.vo.FestivalMapName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageContentType;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageDimensions;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileName;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageFileSize;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import com.example.chookjibupadmin.map.command.domain.vo.Sha256Checksum;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
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
                ),
                @UniqueConstraint(
                        name = "uk_festival_maps_analysis_image_key",
                        columnNames = "analysis_image_key"
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

    @Column(name = "location_id")
    private Long locationId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "map_name", nullable = false, length = 150)
    )
    private FestivalMapName mapName;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "original_file_name", nullable = false, length = 255)
    )
    private MapImageFileName originalFileName;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "source_image_key", nullable = false, length = 700)
    )
    private MapImageObjectKey originalImageKey;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "display_image_key", nullable = false, length = 700)
    )
    private MapImageObjectKey displayImageKey;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "analysis_image_key", nullable = false, length = 700)
    )
    private MapImageObjectKey analysisImageKey;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "source_content_type", nullable = false, length = 50)
    )
    private MapImageContentType originalContentType;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "display_content_type", nullable = false, length = 50)
    )
    private MapImageContentType displayContentType;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "analysis_content_type", nullable = false, length = 50)
    )
    private MapImageContentType analysisContentType;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "source_file_size", nullable = false)
    )
    private MapImageFileSize originalFileSize;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "display_file_size", nullable = false)
    )
    private MapImageFileSize displayFileSize;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "analysis_file_size", nullable = false)
    )
    private MapImageFileSize analysisFileSize;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "width",
                    column = @Column(name = "image_width", nullable = false)
            ),
            @AttributeOverride(
                    name = "height",
                    column = @Column(name = "image_height", nullable = false)
            )
    })
    private MapImageDimensions displayImageDimensions;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "width",
                    column = @Column(name = "analysis_image_width", nullable = false)
            ),
            @AttributeOverride(
                    name = "height",
                    column = @Column(name = "analysis_image_height", nullable = false)
            )
    })
    private MapImageDimensions analysisImageDimensions;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "source_checksum_sha256", nullable = false, length = 64)
    )
    private Sha256Checksum originalChecksumSha256;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "display_checksum_sha256", nullable = false, length = 64)
    )
    private Sha256Checksum displayChecksumSha256;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "analysis_checksum_sha256", nullable = false, length = 64)
    )
    private Sha256Checksum analysisChecksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_status", nullable = false, length = 30)
    private FestivalMapStorageStatus storageStatus;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "created_by_admin_id", nullable = false, updatable = false)
    private Long createdByAdminId;

    @Column(name = "replaces_map_id", updatable = false)
    private Long replacesMapId;

    @Column(name = "replaced_at")
    private LocalDateTime replacedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    private Long version;

    private FestivalMap(
            UUID publicId,
            Long festivalId,
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
            Sha256Checksum analysisChecksumSha256,
            Long createdByAdminId
    ) {
        this.publicId = publicId;
        this.festivalId = festivalId;
        this.mapName = mapName;
        this.originalFileName = originalFileName;
        this.originalImageKey = originalImageKey;
        this.displayImageKey = displayImageKey;
        this.analysisImageKey = analysisImageKey;
        this.originalContentType = originalContentType;
        this.displayContentType = displayContentType;
        this.analysisContentType = analysisContentType;
        this.originalFileSize = originalFileSize;
        this.displayFileSize = displayFileSize;
        this.analysisFileSize = analysisFileSize;
        this.displayImageDimensions = displayImageDimensions;
        this.analysisImageDimensions = analysisImageDimensions;
        this.originalChecksumSha256 = originalChecksumSha256;
        this.displayChecksumSha256 = displayChecksumSha256;
        this.analysisChecksumSha256 = analysisChecksumSha256;
        this.storageStatus = FestivalMapStorageStatus.UPLOADED;
        this.current = true;
        this.createdByAdminId = createdByAdminId;
    }

    public static FestivalMap uploaded(
            UUID publicId,
            Long festivalId,
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
            Sha256Checksum analysisChecksumSha256,
            Long createdByAdminId
    ) {
        if (publicId == null || festivalId == null || createdByAdminId == null
                || mapName == null || originalFileName == null
                || originalImageKey == null || displayImageKey == null
                || analysisImageKey == null || originalContentType == null
                || displayContentType == null || analysisContentType == null
                || originalFileSize == null || displayFileSize == null
                || analysisFileSize == null || displayImageDimensions == null
                || analysisImageDimensions == null
                || originalChecksumSha256 == null
                || displayChecksumSha256 == null
                || analysisChecksumSha256 == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return new FestivalMap(
                publicId,
                festivalId,
                mapName,
                originalFileName,
                originalImageKey,
                displayImageKey,
                analysisImageKey,
                originalContentType,
                displayContentType,
                analysisContentType,
                originalFileSize,
                displayFileSize,
                analysisFileSize,
                displayImageDimensions,
                analysisImageDimensions,
                originalChecksumSha256,
                displayChecksumSha256,
                analysisChecksumSha256,
                createdByAdminId
        );
    }

    /**
     * 새 배치도를 현재 대상으로 전환하고 이 배치도를 교체 이력으로 남긴다.
     */
    public void replaceWith(FestivalMap replacement, LocalDateTime replacedAt) {
        if (replacement == null || replacedAt == null
                || replacement == this || id == null || replacement.id != null
                || publicId.equals(replacement.publicId)
                || !festivalId.equals(replacement.festivalId)
                || storageStatus != FestivalMapStorageStatus.UPLOADED
                || !current
                || replacement.storageStatus
                != FestivalMapStorageStatus.UPLOADED
                || !replacement.current
                || replacement.replacesMapId != null) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }
        storageStatus = FestivalMapStorageStatus.REPLACED;
        current = false;
        this.replacedAt = replacedAt;
        replacement.replacesMapId = id;
    }

    /**
     * 저장소 객체 삭제를 시작할 수 있는 상태로 전환한다.
     */
    public void beginDeletion() {
        if (storageStatus == FestivalMapStorageStatus.DELETING
                || storageStatus == FestivalMapStorageStatus.DELETED) {
            return;
        }
        if (storageStatus != FestivalMapStorageStatus.UPLOADED
                && storageStatus != FestivalMapStorageStatus.REPLACED) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }
        storageStatus = FestivalMapStorageStatus.DELETING;
        current = false;
    }

    /**
     * original, display, analysis 객체가 모두 삭제된 상태로 확정한다.
     */
    public void completeDeletion(LocalDateTime deletedAt) {
        if (storageStatus == FestivalMapStorageStatus.DELETED) {
            return;
        }
        if (storageStatus != FestivalMapStorageStatus.DELETING
                || deletedAt == null) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }
        storageStatus = FestivalMapStorageStatus.DELETED;
        this.deletedAt = deletedAt;
    }

    public boolean belongsTo(Long festivalId) {
        return this.festivalId.equals(festivalId);
    }

    public void assignLocation(Long locationId) {
        if (locationId == null || this.locationId != null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.locationId = locationId;
    }

    /**
     * 현재 화면에 표시할 수 있는 배치도인지 검증한다.
     */
    public void validateReadable() {
        if (storageStatus != FestivalMapStorageStatus.UPLOADED || !current) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }
    }

}
