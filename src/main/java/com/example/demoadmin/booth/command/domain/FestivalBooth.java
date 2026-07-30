package com.example.demoadmin.booth.command.domain;

import com.example.demoadmin.booth.command.domain.vo.BoothCategory;
import com.example.demoadmin.booth.command.domain.vo.BoothDescription;
import com.example.demoadmin.booth.command.domain.vo.BoothLocation;
import com.example.demoadmin.booth.command.domain.vo.BoothName;
import com.example.demoadmin.common.domain.BaseTimeEntity;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
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
 * 축제에 속한 운영 부스 Aggregate이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "festival_booths",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_festival_booths_public_id",
                        columnNames = "public_id"
                )
        }
)
public class FestivalBooth extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "festival_id", nullable = false)
    private Long festivalId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "name", nullable = false, length = 100)
    )
    private BoothName name;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "category", nullable = false, length = 50)
    )
    private BoothCategory category;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "location", nullable = false, length = 255)
    )
    private BoothLocation location;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "description", nullable = false, length = 1000)
    )
    private BoothDescription description;

    @Enumerated(EnumType.STRING)
    @Column(name = "operating_status", nullable = false, length = 50)
    private BoothOperatingStatus operatingStatus;

    @Column(name = "current_queue_line_id")
    private Long currentQueueLineId;

    @Column(name = "expected_waiting_minutes", nullable = false)
    private int expectedWaitingMinutes;

    private FestivalBooth(
            Long festivalId,
            BoothName name,
            BoothCategory category,
            BoothLocation location,
            BoothDescription description
    ) {
        validateFestivalId(festivalId);
        validateBasicInfo(name, category, location, description);

        this.publicId = UUID.randomUUID();
        this.festivalId = festivalId;
        this.name = name;
        this.category = category;
        this.location = location;
        this.description = description;
        this.operatingStatus = BoothOperatingStatus.PREPARING;
        this.expectedWaitingMinutes = 0;
    }

    public static FestivalBooth create(
            Long festivalId,
            BoothName name,
            BoothCategory category,
            BoothLocation location,
            BoothDescription description
    ) {
        return new FestivalBooth(
                festivalId,
                name,
                category,
                location,
                description
        );
    }

    public void updateBasicInfo(
            BoothName name,
            BoothCategory category,
            BoothLocation location,
            BoothDescription description
    ) {
        validateBasicInfo(name, category, location, description);
        this.name = name;
        this.category = category;
        this.location = location;
        this.description = description;
    }

    public void updateQueueTail(BoothQueueLine queueLine) {
        if (operatingStatus.isClosed()) {
            throw new CustomException(ErrorCode.BOOTH_CLOSED);
        }
        if (!queueLine.belongsTo(this)) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_LINE_NOT_BELONG_TO_BOOTH);
        }

        this.currentQueueLineId = queueLine.getId();
        this.expectedWaitingMinutes = queueLine.getExpectedWaitingMinutes();
        this.operatingStatus = BoothOperatingStatus.OPERATING;
    }

    public void saturateWith(BoothQueueLine queueLine) {
        if (operatingStatus.isClosed()) {
            throw new CustomException(ErrorCode.BOOTH_CLOSED);
        }
        if (!queueLine.belongsTo(this)) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_LINE_NOT_BELONG_TO_BOOTH);
        }

        this.currentQueueLineId = queueLine.getId();
        this.expectedWaitingMinutes = queueLine.getExpectedWaitingMinutes();
        this.operatingStatus = BoothOperatingStatus.SATURATED;
    }

    public void close() {
        this.operatingStatus = BoothOperatingStatus.CLOSED;
        this.currentQueueLineId = null;
        this.expectedWaitingMinutes = 0;
    }

    public void refreshCurrentQueueLine(BoothQueueLine queueLine) {
        if (queueLine == null || !queueLine.belongsTo(this)) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_LINE_NOT_BELONG_TO_BOOTH);
        }
        if (currentQueueLineId != null && currentQueueLineId.equals(queueLine.getId())) {
            this.expectedWaitingMinutes = queueLine.getExpectedWaitingMinutes();
        }
    }

    private static void validateFestivalId(Long festivalId) {
        if (festivalId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateBasicInfo(
            BoothName name,
            BoothCategory category,
            BoothLocation location,
            BoothDescription description
    ) {
        if (name == null || category == null || location == null || description == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    public String getNameValue() {
        return name.getValue();
    }

    public String getCategoryValue() {
        return category.getValue();
    }

    public String getLocationValue() {
        return location.getValue();
    }

    public String getDescriptionValue() {
        return description.getValue();
    }
}
