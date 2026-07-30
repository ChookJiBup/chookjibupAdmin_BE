package com.example.demoadmin.booth.command.domain;

import com.example.demoadmin.booth.command.domain.vo.BoothLineLabel;
import com.example.demoadmin.common.domain.BaseTimeEntity;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
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
 * 특정 부스에 종속된 대기 라인이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "booth_queue_lines",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_booth_queue_lines_public_id",
                        columnNames = "public_id"
                ),
                @UniqueConstraint(
                        name = "uk_booth_queue_lines_booth_order",
                        columnNames = {"booth_id", "line_order"}
                )
        }
)
public class BoothQueueLine extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "booth_id", nullable = false)
    private Long boothId;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "label", nullable = false, length = 100)
    )
    private BoothLineLabel label;

    @Column(name = "expected_waiting_minutes", nullable = false)
    private int expectedWaitingMinutes;

    @Column(name = "max_capacity", nullable = false)
    private int maxCapacity;

    @Column(name = "path_data", nullable = false, length = 4000)
    private String pathData;

    @Column(name = "entry_point_data", nullable = false, length = 1000)
    private String entryPointData;

    private BoothQueueLine(
            Long boothId,
            int lineOrder,
            BoothLineLabel label,
            int expectedWaitingMinutes,
            int maxCapacity,
            String pathData,
            String entryPointData
    ) {
        validateBoothId(boothId);
        validateLabel(label);
        validatePositive(lineOrder);
        validateLabel(label);
        validateNotNegative(expectedWaitingMinutes);
        validateNotNegative(maxCapacity);

        this.publicId = UUID.randomUUID();
        this.boothId = boothId;
        this.lineOrder = lineOrder;
        this.label = label;
        this.expectedWaitingMinutes = expectedWaitingMinutes;
        this.maxCapacity = maxCapacity;
        this.pathData = normalizeData(pathData);
        this.entryPointData = normalizeData(entryPointData);
    }

    public static BoothQueueLine create(
            Long boothId,
            int lineOrder,
            BoothLineLabel label,
            int expectedWaitingMinutes,
            int maxCapacity,
            String pathData,
            String entryPointData
    ) {
        return new BoothQueueLine(
                boothId,
                lineOrder,
                label,
                expectedWaitingMinutes,
                maxCapacity,
                pathData,
                entryPointData
        );
    }

    public void update(
            int lineOrder,
            BoothLineLabel label,
            int expectedWaitingMinutes,
            int maxCapacity,
            String pathData,
            String entryPointData
    ) {
        validatePositive(lineOrder);
        validateNotNegative(expectedWaitingMinutes);
        validateNotNegative(maxCapacity);

        this.lineOrder = lineOrder;
        this.label = label;
        this.expectedWaitingMinutes = expectedWaitingMinutes;
        this.maxCapacity = maxCapacity;
        this.pathData = normalizeData(pathData);
        this.entryPointData = normalizeData(entryPointData);
    }

    public boolean belongsTo(FestivalBooth booth) {
        return booth != null && boothId.equals(booth.getId());
    }

    private static void validateBoothId(Long boothId) {
        if (boothId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateLabel(BoothLineLabel label) {
        if (label == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static void validatePositive(int value) {
        if (value <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateNotNegative(int value) {
        if (value < 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static String normalizeData(String value) {
        if (value == null || value.isBlank()) {
            return "{}";
        }

        return value;
    }

    public String getLabelValue() {
        return label.getValue();
    }
}
