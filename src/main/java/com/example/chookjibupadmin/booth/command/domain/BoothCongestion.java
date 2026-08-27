package com.example.chookjibupadmin.booth.command.domain;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부스 혼잡 이력이다. append-only로 생성만 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "booth_congestion")
public class BoothCongestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "congestion_id")
    private Long id;

    @Column(name = "booth_id", nullable = false, updatable = false)
    private Long boothId;

    @Enumerated(EnumType.STRING)
    @Column(name = "modifier_type", nullable = false, length = 20, updatable = false)
    private BoothCongestionModifierType modifierType;

    @Column(name = "modifier_admin_id", updatable = false)
    private Long modifierAdminId;

    @Column(name = "modifier_staff_id", updatable = false)
    private Long modifierStaffId;

    @Column(name = "wait_minutes")
    private Integer waitMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "congestion_level", nullable = false, length = 20, updatable = false)
    private BoothCongestionLevel congestionLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static BoothCongestion recordByAdmin(
            Long boothId,
            Long adminId,
            int waitMinutes,
            BoothCongestionLevel level
    ) {
        return create(
                boothId,
                BoothCongestionModifierType.ADMIN,
                adminId,
                null,
                waitMinutes,
                level
        );
    }

    public static BoothCongestion recordByStaff(
            Long boothId,
            Long staffId,
            int waitMinutes,
            BoothCongestionLevel level
    ) {
        return create(
                boothId,
                BoothCongestionModifierType.STAFF,
                null,
                staffId,
                waitMinutes,
                level
        );
    }

    private static BoothCongestion create(
            Long boothId,
            BoothCongestionModifierType type,
            Long adminId,
            Long staffId,
            int waitMinutes,
            BoothCongestionLevel level
    ) {
        if (boothId == null || level == null || waitMinutes < 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (type == BoothCongestionModifierType.ADMIN && adminId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (type == BoothCongestionModifierType.STAFF && staffId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        LocalDateTime now = LocalDateTime.now();
        BoothCongestion row = new BoothCongestion();
        row.boothId = boothId;
        row.modifierType = type;
        row.modifierAdminId = adminId;
        row.modifierStaffId = staffId;
        row.waitMinutes = waitMinutes;
        row.congestionLevel = level;
        row.createdAt = now;
        row.updatedAt = now;
        return row;
    }
}
