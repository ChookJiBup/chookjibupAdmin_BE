package com.example.chookjibupadmin.map.command.domain;

import com.example.chookjibupadmin.common.domain.BaseTimeEntity;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageAnchor;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageObjectKey;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 축제 지도의 카카오맵 표시 설정(부지 경계·오버레이)을 보관한다.
 */
@Entity
@Getter
@Table(
        name = "festival_map_presentation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_festival_map_presentation_map",
                columnNames = "map_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalMapPresentation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "map_id", nullable = false, updatable = false)
    private Long mapId;

    @Column(name = "festival_id", nullable = false, updatable = false)
    private Long festivalId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "boundary_geometry", columnDefinition = "jsonb")
    private String boundaryGeometry;

    /**
     * 오버레이 이미지가 없을 때는 null embed로 둔다(VO는 빈 문자열을 허용하지 않음).
     */
    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "overlay_image_key", length = 512)
    )
    private MapImageObjectKey overlayImageKey;

    @Column(name = "overlay_asset_id")
    private UUID overlayAssetId;

    @Column(name = "overlay_image_width")
    private Integer overlayImageWidth;

    @Column(name = "overlay_image_height")
    private Integer overlayImageHeight;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "centerLatitude",
                    column = @Column(name = "overlay_center_lat", precision = 10, scale = 7)
            ),
            @AttributeOverride(
                    name = "centerLongitude",
                    column = @Column(name = "overlay_center_lng", precision = 10, scale = 7)
            ),
            @AttributeOverride(
                    name = "groundWidthMeters",
                    column = @Column(name = "overlay_ground_width_m", precision = 10, scale = 2)
            ),
            @AttributeOverride(
                    name = "rotationDegrees",
                    column = @Column(name = "overlay_rotation_deg", precision = 6, scale = 3)
            )
    })
    private MapImageAnchor overlayImageAnchor;

    @Column(name = "overlay_opacity", nullable = false, precision = 3, scale = 2)
    private BigDecimal overlayOpacity;

    @Column(name = "overlay_visible", nullable = false)
    private boolean overlayVisible;

    @Column(name = "clip_to_boundary", nullable = false)
    private boolean clipToBoundary;

    public static FestivalMapPresentation createEmpty(Long mapId, Long festivalId) {
        if (mapId == null || festivalId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        FestivalMapPresentation presentation = new FestivalMapPresentation();
        presentation.mapId = mapId;
        presentation.festivalId = festivalId;
        presentation.overlayOpacity = new BigDecimal("1.00");
        presentation.overlayVisible = false;
        presentation.clipToBoundary = false;
        return presentation;
    }

    public void updateBoundary(String boundaryGeometryJson) {
        if (boundaryGeometryJson == null || boundaryGeometryJson.isBlank()) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID);
        }
        this.boundaryGeometry = boundaryGeometryJson;
    }

    public void clearBoundary() {
        this.boundaryGeometry = null;
        if (this.clipToBoundary) {
            this.clipToBoundary = false;
        }
    }

    public void updateOverlay(
            MapImageObjectKey imageKey,
            UUID assetId,
            Integer imageWidth,
            Integer imageHeight,
            MapImageAnchor anchor
    ) {
        if (imageKey == null || assetId == null
                || imageWidth == null || imageHeight == null
                || imageWidth <= 0 || imageHeight <= 0) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_OVERLAY_INVALID);
        }
        this.overlayImageKey = imageKey;
        this.overlayAssetId = assetId;
        this.overlayImageWidth = imageWidth;
        this.overlayImageHeight = imageHeight;
        if (anchor != null) {
            this.overlayImageAnchor = anchor;
        }
    }

    public void clearOverlay() {
        this.overlayImageKey = null;
        this.overlayAssetId = null;
        this.overlayImageWidth = null;
        this.overlayImageHeight = null;
        this.overlayImageAnchor = null;
        this.overlayVisible = false;
    }

    public void updateOverlayAnchor(MapImageAnchor anchor) {
        this.overlayImageAnchor = anchor;
    }

    public void setOverlayVisible(boolean visible) {
        if (visible && overlayImageKey == null) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_OVERLAY_INVALID);
        }
        this.overlayVisible = visible;
    }

    public void setClipToBoundary(boolean clip) {
        if (clip && (boundaryGeometry == null || boundaryGeometry.isBlank())) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_OVERLAY_INVALID);
        }
        this.clipToBoundary = clip;
    }

    public void setOverlayOpacity(BigDecimal opacity) {
        if (opacity == null
                || opacity.compareTo(BigDecimal.ZERO) < 0
                || opacity.compareTo(BigDecimal.ONE) > 0) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_OVERLAY_INVALID);
        }
        this.overlayOpacity = opacity.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean hasOverlayImage() {
        return overlayImageKey != null;
    }

    public boolean hasBoundary() {
        return boundaryGeometry != null && !boundaryGeometry.isBlank();
    }

    public boolean belongsTo(Long festivalId) {
        return this.festivalId.equals(festivalId);
    }
}
