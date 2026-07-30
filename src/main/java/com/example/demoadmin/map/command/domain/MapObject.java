package com.example.demoadmin.map.command.domain;

import com.example.demoadmin.common.domain.BaseTimeEntity;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.domain.vo.ConfidenceScore;
import com.example.demoadmin.map.command.domain.vo.GeometryData;
import com.example.demoadmin.map.command.domain.vo.MapObjectName;
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
 * 배치도 이미지에서 탐지되거나 관리자가 생성한 지도 객체이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "map_objects",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_map_objects_public_id",
                        columnNames = "public_id"
                )
        }
)
public class MapObject extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "festival_map_id", nullable = false)
    private Long festivalMapId;

    @Column(name = "analysis_job_id", nullable = false)
    private Long analysisJobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private MapObjectType type;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "name", nullable = false, length = 100)
    )
    private MapObjectName name;

    @Enumerated(EnumType.STRING)
    @Column(name = "geometry_type", nullable = false, length = 50)
    private GeometryType geometryType;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "geometry_data", nullable = false, length = 4000)
    )
    private GeometryData geometryData;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "confidence", nullable = false)
    )
    private ConfidenceScore confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 50)
    private MapReviewStatus reviewStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private MapDetectionSource source;

    private MapObject(
            Long festivalMapId,
            Long analysisJobId,
            MapObjectType type,
            MapObjectName name,
            GeometryType geometryType,
            GeometryData geometryData,
            ConfidenceScore confidence,
            MapDetectionSource source
    ) {
        validateId(festivalMapId);
        validateId(analysisJobId);
        validateRequired(type);
        validateRequired(name);
        validateRequired(geometryType);
        validateRequired(geometryData);
        validateRequired(confidence);
        validateRequired(source);

        this.publicId = UUID.randomUUID();
        this.festivalMapId = festivalMapId;
        this.analysisJobId = analysisJobId;
        this.type = type;
        this.name = name;
        this.geometryType = geometryType;
        this.geometryData = geometryData;
        this.confidence = confidence;
        this.reviewStatus = MapReviewStatus.REVIEW_REQUIRED;
        this.source = source;
    }

    public static MapObject createAiGenerated(
            Long festivalMapId,
            Long analysisJobId,
            MapObjectType type,
            MapObjectName name,
            GeometryType geometryType,
            GeometryData geometryData,
            ConfidenceScore confidence
    ) {
        return new MapObject(
                festivalMapId,
                analysisJobId,
                type,
                name,
                geometryType,
                geometryData,
                confidence,
                MapDetectionSource.AI_GENERATED
        );
    }

    public void confirm() {
        if (reviewStatus == MapReviewStatus.REJECTED) {
            throw new CustomException(ErrorCode.MAP_OBJECT_REVIEW_STATUS_INVALID);
        }

        this.reviewStatus = MapReviewStatus.CONFIRMED;
    }

    public void modify(
            MapObjectType type,
            MapObjectName name,
            GeometryType geometryType,
            GeometryData geometryData
    ) {
        if (reviewStatus == MapReviewStatus.REJECTED) {
            throw new CustomException(ErrorCode.MAP_OBJECT_REVIEW_STATUS_INVALID);
        }
        validateRequired(type);
        validateRequired(name);
        validateRequired(geometryType);
        validateRequired(geometryData);

        this.type = type;
        this.name = name;
        this.geometryType = geometryType;
        this.geometryData = geometryData;
        this.reviewStatus = MapReviewStatus.MODIFIED;
    }

    public void reject() {
        this.reviewStatus = MapReviewStatus.REJECTED;
    }

    private static void validateId(Long id) {
        if (id == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateRequired(Object value) {
        if (value == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    public String getNameValue() {
        return name.getValue();
    }

    public String getGeometryDataValue() {
        return geometryData.getValue();
    }

    public double getConfidenceValue() {
        return confidence.getValue();
    }
}
