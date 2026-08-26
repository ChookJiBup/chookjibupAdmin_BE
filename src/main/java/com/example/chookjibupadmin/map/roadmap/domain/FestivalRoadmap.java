package com.example.chookjibupadmin.map.roadmap.domain;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(name = "festival_roadmap", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_festival_roadmap_public_id",
                columnNames = "public_id"
        ),
        @UniqueConstraint(
                name = "uk_festival_roadmap_festival_id",
                columnNames = "festival_id"
        )
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalRoadmap extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "festival_id", nullable = false, updatable = false)
    private Long festivalId;

    @Column(name = "current_map_id", nullable = false)
    private Long currentMapId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoadmapStatus status;

    @Column(name = "edit_revision", nullable = false)
    private long editRevision;

    @Column(name = "published_version", nullable = false)
    private long publishedVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "zones", nullable = false, columnDefinition = "jsonb")
    private List<RoadmapZone> zones = new ArrayList<>();

    @Column(name = "created_by_admin_id", nullable = false, updatable = false)
    private Long createdByAdminId;

    @Version
    private Long version;

    public static FestivalRoadmap create(Long festivalId, Long mapId, Long adminId) {
        FestivalRoadmap roadmap = new FestivalRoadmap();
        roadmap.publicId = UUID.randomUUID();
        roadmap.festivalId = festivalId;
        roadmap.currentMapId = mapId;
        roadmap.createdByAdminId = adminId;
        roadmap.status = RoadmapStatus.ANALYZING;
        return roadmap;
    }

    public void replaceMap(Long mapId) {
        currentMapId = mapId;
        status = RoadmapStatus.ANALYZING;
        editRevision++;
    }

    public void analysisCompleted() {
        status = RoadmapStatus.REVIEW_REQUIRED;
        editRevision++;
    }

    /** 카카오맵 위경도 편집용 roadmap을 EDITING 상태로 생성한다. */
    public static FestivalRoadmap createForCoordinateMap(
            Long festivalId,
            Long mapId,
            Long adminId
    ) {
        FestivalRoadmap roadmap = new FestivalRoadmap();
        roadmap.publicId = UUID.randomUUID();
        roadmap.festivalId = festivalId;
        roadmap.currentMapId = mapId;
        roadmap.createdByAdminId = adminId;
        roadmap.status = RoadmapStatus.EDITING;
        roadmap.editRevision = 0;
        return roadmap;
    }

    public long applyAdminEdit(long baseRevision) {
        if (editRevision != baseRevision) {
            throw new CustomException(ErrorCode.ROADMAP_REVISION_CONFLICT);
        }
        if (status == RoadmapStatus.ANALYZING) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }

        status = RoadmapStatus.EDITING;
        return ++editRevision;
    }

    public void replaceZones(List<RoadmapZone> zones) {
        this.zones = new ArrayList<>(zones == null ? List.of() : zones);
    }

    public List<RoadmapZone> getZones() {
        return Collections.unmodifiableList(zones == null ? List.of() : zones);
    }
}
