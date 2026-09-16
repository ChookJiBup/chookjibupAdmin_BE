package com.example.chookjibupadmin.booth.command.domain;

import com.example.chookjibupadmin.common.domain.BaseTimeEntity;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 사전 동선이다. 저장 자체는 실제 대기나 혼잡 이력을 생성하지 않는다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "booth_queue_plan", uniqueConstraints = @UniqueConstraint(columnNames = "booth_id"))
public class BoothQueuePlan extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    private UUID publicId;
    @Column(name = "festival_id", nullable = false, updatable = false)
    private Long festivalId;
    @Column(name = "booth_id", nullable = false, updatable = false)
    private Long boothId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "path_geometry", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, BigDecimal>> pathGeometry;
    @Column(name = "length_meters", nullable = false)
    private double lengthMeters;
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "metersPerPerson", column = @Column(name = "meters_per_person", nullable = false)),
        @AttributeOverride(name = "servedPersonsPerMinute", column = @Column(name = "served_persons_per_minute", nullable = false))
    })
    private QueueEstimationSettings settings;
    @Column(name = "source_node_id")
    private UUID sourceNodeId;
    @Column(name = "modifier_admin_id")
    private Long modifierAdminId;
    @Column(name = "revision", nullable = false)
    private long revision;

    public static BoothQueuePlan create(Long festivalId, Long boothId) {
        if (festivalId == null || boothId == null) throw new IllegalArgumentException();
        BoothQueuePlan plan = new BoothQueuePlan();
        plan.publicId = UUID.randomUUID(); plan.festivalId = festivalId; plan.boothId = boothId;
        return plan;
    }

    /** 부스 락을 보유한 트랜잭션에서 기대 revision을 검사하고 계획을 교체한다. */
    public void replace(List<Map<String, BigDecimal>> path, QueueEstimationSettings settings,
            UUID sourceNodeId, Long adminId, long expectedRevision) {
        if (revision != expectedRevision) throw new CustomException(ErrorCode.BOOTH_QUEUE_REVISION_CONFLICT);
        if (settings == null || adminId == null) throw new IllegalArgumentException();
        this.pathGeometry = QueueGeometry.validate(path);
        this.lengthMeters = QueueGeometry.length(pathGeometry);
        this.settings = settings; this.sourceNodeId = sourceNodeId; this.modifierAdminId = adminId;
        revision++;
    }

    public List<Map<String, BigDecimal>> getPathGeometry() { return List.copyOf(pathGeometry); }
}
