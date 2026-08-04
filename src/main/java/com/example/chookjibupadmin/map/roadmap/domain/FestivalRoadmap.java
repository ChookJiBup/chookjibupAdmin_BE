package com.example.chookjibupadmin.map.roadmap.domain;

import com.example.chookjibupadmin.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Getter
@Table(name = "festival_roadmap", uniqueConstraints = {
        @UniqueConstraint(name="uk_festival_roadmap_public_id", columnNames="public_id"),
        @UniqueConstraint(name="uk_festival_roadmap_festival_id", columnNames="festival_id")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalRoadmap extends BaseTimeEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="public_id", nullable=false, updatable=false) private UUID publicId;
    @Column(name="festival_id", nullable=false, updatable=false) private Long festivalId;
    @Column(name="current_map_id", nullable=false) private Long currentMapId;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private RoadmapStatus status;
    @Column(name="edit_revision", nullable=false) private long editRevision;
    @Column(name="published_version", nullable=false) private long publishedVersion;
    @Column(name="created_by_admin_id", nullable=false, updatable=false) private Long createdByAdminId;
    @Version private Long version;

    public static FestivalRoadmap create(Long festivalId, Long mapId, Long adminId) {
        FestivalRoadmap roadmap = new FestivalRoadmap();
        roadmap.publicId = UUID.randomUUID(); roadmap.festivalId = festivalId;
        roadmap.currentMapId = mapId; roadmap.createdByAdminId = adminId;
        roadmap.status = RoadmapStatus.ANALYZING;
        return roadmap;
    }
    public void replaceMap(Long mapId) { currentMapId = mapId; status = RoadmapStatus.ANALYZING; editRevision++; }
    public void analysisCompleted() { status = RoadmapStatus.REVIEW_REQUIRED; editRevision++; }
}
