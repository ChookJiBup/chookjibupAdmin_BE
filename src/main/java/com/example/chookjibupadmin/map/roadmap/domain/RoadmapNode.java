package com.example.chookjibupadmin.map.roadmap.domain;

import com.example.chookjibupadmin.common.domain.BaseTimeEntity;
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
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(
        name = "roadmap_node",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roadmap_node_public_id",
                columnNames = "public_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadmapNode extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "roadmap_id", nullable = false, updatable = false)
    private Long roadmapId;

    @Column(name = "map_id", nullable = false, updatable = false)
    private Long mapId;

    @Column(name = "analysis_job_id", updatable = false)
    private Long analysisJobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 30)
    private NodeType nodeType;

    @Column(name = "node_name", nullable = false, length = 150)
    private String nodeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "geometry_type", nullable = false, length = 20)
    private GeometryType geometryType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "geometry_data", nullable = false, columnDefinition = "jsonb")
    private String geometryData;

    @Column(name = "geometry_schema_version", nullable = false, length = 30)
    private String geometrySchemaVersion;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "recognized_text", length = 500)
    private String recognizedText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NodeSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private NodeReviewStatus reviewStatus;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "last_modified_by_admin_id")
    private Long lastModifiedByAdminId;

    @Version
    private Long version;

    public static RoadmapNode ai(
            Long roadmapId,
            Long mapId,
            Long jobId,
            NodeType type,
            String name,
            GeometryType geometryType,
            String geometryData,
            BigDecimal confidence,
            String recognizedText,
            int sortOrder
    ) {
        RoadmapNode node = new RoadmapNode();
        node.publicId = UUID.randomUUID();
        node.roadmapId = roadmapId;
        node.mapId = mapId;
        node.analysisJobId = jobId;
        node.nodeType = type;
        node.nodeName = name;
        node.geometryType = geometryType;
        node.geometryData = geometryData;
        node.geometrySchemaVersion = "1.0";
        node.confidence = confidence;
        node.recognizedText = recognizedText;
        node.source = NodeSource.AI;
        node.reviewStatus = NodeReviewStatus.REVIEW_REQUIRED;
        node.sortOrder = sortOrder;
        return node;
    }

    public static RoadmapNode admin(
            Long roadmapId,
            Long mapId,
            NodeType type,
            String name,
            GeometryType geometryType,
            String geometryData,
            int sortOrder,
            Long adminId
    ) {
        RoadmapNode node = new RoadmapNode();
        node.publicId = UUID.randomUUID();
        node.roadmapId = roadmapId;
        node.mapId = mapId;
        node.nodeType = type;
        node.nodeName = name;
        node.geometryType = geometryType;
        node.geometryData = geometryData;
        node.geometrySchemaVersion = "1.0";
        node.source = NodeSource.ADMIN;
        node.reviewStatus = NodeReviewStatus.CONFIRMED;
        node.sortOrder = sortOrder;
        node.createdByAdminId = adminId;
        node.lastModifiedByAdminId = adminId;
        return node;
    }

    public void updateByAdmin(
            NodeType type,
            String name,
            GeometryType geometryType,
            String geometryData,
            int sortOrder,
            Long adminId
    ) {
        nodeType = type;
        nodeName = name;
        this.geometryType = geometryType;
        this.geometryData = geometryData;
        this.sortOrder = sortOrder;
        reviewStatus = NodeReviewStatus.CONFIRMED;
        lastModifiedByAdminId = adminId;
    }
}
