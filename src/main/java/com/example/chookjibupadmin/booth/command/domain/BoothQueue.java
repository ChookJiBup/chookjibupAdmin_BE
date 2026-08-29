package com.example.chookjibupadmin.booth.command.domain;

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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 승인 부스의 대기열(줄끝) 운영 상태이다. 부스당 최대 1행이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "booth_queue",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_booth_queue_public_id", columnNames = "public_id"),
                @UniqueConstraint(name = "uk_booth_queue_booth_id", columnNames = "booth_id")
        }
)
public class BoothQueue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "queue_id")
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "festival_id", nullable = false, updatable = false)
    private Long festivalId;

    @Column(name = "booth_id", nullable = false, updatable = false)
    private Long boothId;

    @Column(name = "tail_latitude", precision = 10, scale = 7)
    private BigDecimal tailLatitude;

    @Column(name = "tail_longitude", precision = 10, scale = 7)
    private BigDecimal tailLongitude;

    @Column(name = "queue_tail_meters")
    private Integer queueTailMeters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "path_geometry", columnDefinition = "jsonb")
    private List<Map<String, BigDecimal>> pathGeometry;

    @Enumerated(EnumType.STRING)
    @Column(name = "modifier_type", length = 20)
    private BoothQueueModifierType modifierType;

    @Column(name = "modifier_admin_id")
    private Long modifierAdminId;

    @Column(name = "modifier_staff_id")
    private Long modifierStaffId;

    public static BoothQueue createEmpty(Long festivalId, Long boothId) {
        if (festivalId == null || boothId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        BoothQueue queue = new BoothQueue();
        queue.publicId = UUID.randomUUID();
        queue.festivalId = festivalId;
        queue.boothId = boothId;
        return queue;
    }

    public void updateTail(
            BigDecimal tailLatitude,
            BigDecimal tailLongitude,
            Integer queueTailMeters,
            List<Map<String, BigDecimal>> pathGeometry,
            BoothQueueModifierType modifierType,
            Long modifierAdminId,
            Long modifierStaffId
    ) {
        if (tailLatitude == null || tailLongitude == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (queueTailMeters != null && queueTailMeters < 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (modifierType == BoothQueueModifierType.ADMIN && modifierAdminId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (modifierType == BoothQueueModifierType.STAFF && modifierStaffId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.tailLatitude = tailLatitude;
        this.tailLongitude = tailLongitude;
        this.queueTailMeters = queueTailMeters;
        this.pathGeometry = pathGeometry;
        this.modifierType = modifierType;
        this.modifierAdminId = modifierType == BoothQueueModifierType.ADMIN ? modifierAdminId : null;
        this.modifierStaffId = modifierType == BoothQueueModifierType.STAFF ? modifierStaffId : null;
    }

    public boolean belongsTo(Long festivalId) {
        return this.festivalId.equals(festivalId);
    }
}
