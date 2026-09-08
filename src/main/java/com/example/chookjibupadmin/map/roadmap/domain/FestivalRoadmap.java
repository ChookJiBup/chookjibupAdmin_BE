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

    /**
     * 분석이 최종 실패하거나 취소되면 편집 가능한 상태로 되돌린다.
     * ANALYZING으로 남겨두면 관리자가 부스를 영영 저장할 수 없다({@link #applyAdminEdit}).
     * 노드가 바뀌지 않았으므로 editRevision은 올리지 않는다.
     */
    public void analysisAborted() {
        if (status == RoadmapStatus.ANALYZING) {
            status = RoadmapStatus.EDITING;
        }
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

    /**
     * 방문객 앱에 배치도를 공개한다.
     *
     * <p>사용자 백엔드는 이 상태가 PUBLISHED일 때만 부스·구역·부지 경계·팜플렛을 내려준다.
     * 분석 중에는 AI가 노드를 통째로 갈아끼우므로, 그 결과를 검수하기 전에 공개하면
     * 방문객이 검수도 안 된 배치를 보게 된다. 그래서 분석 중에는 거부한다.</p>
     *
     * <p>공개한 뒤에도 {@link #applyAdminEdit}이 상태를 EDITING으로 되돌린다. 저장할 때마다
     * 자동으로 다시 감춰지는 셈이라, 편집 내용을 방문객에게 보이려면 저장 후 다시 공개해야 한다.</p>
     */
    public void publish() {
        ensurePublishable();
        status = RoadmapStatus.PUBLISHED;
        publishedVersion = editRevision;
    }

    /** 공개 전 조건 검사만 한다. 호출자가 더 무거운 검사를 하기 전에 먼저 걸러낼 때 쓴다. */
    public void ensurePublishable() {
        if (status == RoadmapStatus.ANALYZING) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_INVALID_STATUS);
        }
    }

    public boolean isPublished() {
        return status == RoadmapStatus.PUBLISHED;
    }

    public void replaceZones(List<RoadmapZone> zones) {
        this.zones = new ArrayList<>(zones == null ? List.of() : zones);
    }

    public List<RoadmapZone> getZones() {
        return Collections.unmodifiableList(zones == null ? List.of() : zones);
    }
}
