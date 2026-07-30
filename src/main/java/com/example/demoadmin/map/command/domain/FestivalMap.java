package com.example.demoadmin.map.command.domain;

import com.example.demoadmin.common.domain.BaseTimeEntity;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.domain.vo.MapFileName;
import com.example.demoadmin.map.command.domain.vo.MapStoragePath;
import jakarta.persistence.AttributeOverride;
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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제 배치도 원본 이미지와 분석 진행 상태를 관리한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "festival_maps",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_festival_maps_public_id",
                        columnNames = "public_id"
                )
        }
)
public class FestivalMap extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "festival_id", nullable = false)
    private Long festivalId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "original_file_name", nullable = false, length = 255)
    )
    private MapFileName originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 50)
    private MapStorageType storageType;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "storage_path", nullable = false, length = 1000)
    )
    private MapStoragePath storagePath;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FestivalMapStatus status;

    private FestivalMap(
            Long festivalId,
            MapFileName originalFileName,
            MapStorageType storageType,
            MapStoragePath storagePath,
            int width,
            int height
    ) {
        validateFestivalId(festivalId);
        validateRequired(originalFileName);
        validateStorageType(storageType);
        validateRequired(storagePath);
        validateDimension(width);
        validateDimension(height);

        this.publicId = UUID.randomUUID();
        this.festivalId = festivalId;
        this.originalFileName = originalFileName;
        this.storageType = storageType;
        this.storagePath = storagePath;
        this.width = width;
        this.height = height;
        this.status = FestivalMapStatus.DRAFT;
    }

    public static FestivalMap create(
            Long festivalId,
            MapFileName originalFileName,
            MapStorageType storageType,
            MapStoragePath storagePath,
            int width,
            int height
    ) {
        return new FestivalMap(
                festivalId,
                originalFileName,
                storageType,
                storagePath,
                width,
                height
        );
    }

    public void markAnalyzed() {
        if (status == FestivalMapStatus.ARCHIVED) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_STATUS_INVALID);
        }

        this.status = FestivalMapStatus.ANALYZED;
    }

    public void confirm() {
        if (status != FestivalMapStatus.ANALYZED) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_STATUS_INVALID);
        }

        this.status = FestivalMapStatus.CONFIRMED;
    }

    public void archive() {
        this.status = FestivalMapStatus.ARCHIVED;
    }

    private static void validateFestivalId(Long festivalId) {
        if (festivalId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateStorageType(MapStorageType storageType) {
        if (storageType == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateRequired(Object value) {
        if (value == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateDimension(int value) {
        if (value <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    public String getOriginalFileNameValue() {
        return originalFileName.getValue();
    }

    public String getStoragePathValue() {
        return storagePath.getValue();
    }
}
