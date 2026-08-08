package com.example.chookjibupadmin.festival.command.domain;

import com.example.chookjibupadmin.common.domain.BaseTimeEntity;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제 기본 정보를 저장하는 Aggregate이다.
 */
@Entity
@Getter
@Table(
        name = "festivals",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_festivals_public_id",
                        columnNames = "public_id"
                ),
                @UniqueConstraint(
                        name = "uk_festivals_series_year",
                        columnNames = {"series_id", "festival_year"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Festival extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO(festival): 현재 축제 저장 필드는 임시 기준이며 추후 DB 설계서와 API 정보를 확인한 뒤 필드/제약/응답을 수정한다.
    // TODO(festival): 운영 DB 반영 전 기존 festivals 데이터의 public_id, series_id, series_public_id, festival_year 백필 마이그레이션을 작성한다.
    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "series_id", nullable = false)
    private Long seriesId;

    @Column(name = "series_public_id", nullable = false, updatable = false)
    private UUID seriesPublicId;

    @Column(name = "festival_year", nullable = false)
    private int year;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "name", nullable = false, length = 100)
    )
    private FestivalName name;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "description", nullable = false, length = 1000)
    )
    private FestivalDescription description;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "address", nullable = false, length = 255)
    )
    private FestivalAddress address;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "detail_address", length = 100)
    )
    // TODO(festival): 운영 배포 전 festivals.detail_address 컬럼 마이그레이션을 작성한다.
    private FestivalDetailAddress detailAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "startDate",
                    column = @Column(name = "start_date", nullable = false)
            ),
            @AttributeOverride(
                    name = "endDate",
                    column = @Column(name = "end_date", nullable = false)
            )
    })
    private FestivalPeriod period;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "startTime",
                    column = @Column(name = "operation_start_time", nullable = false)
            ),
            @AttributeOverride(
                    name = "endTime",
                    column = @Column(name = "operation_end_time", nullable = false)
            )
    })
    private FestivalOperationTime operationTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FestivalStatus status;

    private Festival(
            UUID publicId,
            Long seriesId,
            UUID seriesPublicId,
            int year,
            FestivalName name,
            FestivalDescription description,
            FestivalAddress address,
            FestivalDetailAddress detailAddress,
            FestivalPeriod period,
            FestivalOperationTime operationTime
    ) {
        this.publicId = publicId;
        this.seriesId = seriesId;
        this.seriesPublicId = seriesPublicId;
        this.year = year;
        this.name = name;
        this.description = description;
        this.address = address;
        this.detailAddress = detailAddress;
        this.period = period;
        this.operationTime = operationTime;
        this.status = FestivalStatus.DRAFT;
    }

    /**
     * 임시 기준의 축제 기본 정보를 생성한다.
     */
    public static Festival create(
            Long seriesId,
            UUID seriesPublicId,
            FestivalName name,
            FestivalDescription description,
            FestivalAddress address,
            FestivalDetailAddress detailAddress,
            FestivalPeriod period,
            FestivalOperationTime operationTime
    ) {
        validateSeriesId(seriesId);
        validateSeriesPublicId(seriesPublicId);

        return new Festival(
                UUID.randomUUID(),
                seriesId,
                seriesPublicId,
                period.getStartDate().getYear(),
                name,
                description,
                address,
                detailAddress,
                period,
                operationTime
        );
    }

    /**
     * 외부 저장소 Object Key와 동일한 축제 UUID로 기본 정보를 생성한다.
     */
    public static Festival create(
            UUID publicId,
            Long seriesId,
            UUID seriesPublicId,
            FestivalName name,
            FestivalDescription description,
            FestivalAddress address,
            FestivalDetailAddress detailAddress,
            FestivalPeriod period,
            FestivalOperationTime operationTime
    ) {
        if (publicId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        validateSeriesId(seriesId);
        validateSeriesPublicId(seriesPublicId);
        return new Festival(
                publicId,
                seriesId,
                seriesPublicId,
                period.getStartDate().getYear(),
                name,
                description,
                address,
                detailAddress,
                period,
                operationTime
        );
    }

    /**
     * 상세주소 없이 축제 기본 정보를 생성한다.
     */
    public static Festival create(
            Long seriesId,
            UUID seriesPublicId,
            FestivalName name,
            FestivalDescription description,
            FestivalAddress address,
            FestivalPeriod period,
            FestivalOperationTime operationTime
    ) {
        return create(
                seriesId,
                seriesPublicId,
                name,
                description,
                address,
                FestivalDetailAddress.of(null),
                period,
                operationTime
        );
    }

    private static void validateSeriesId(Long seriesId) {
        if (seriesId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateSeriesPublicId(UUID seriesPublicId) {
        if (seriesPublicId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    /**
     * 임시 기준의 축제 기본 정보를 수정한다.
     */
    public void updateBasicInfo(
            FestivalName name,
            FestivalDescription description,
            FestivalAddress address,
            FestivalDetailAddress detailAddress,
            FestivalPeriod period,
            FestivalOperationTime operationTime
    ) {
        if (!status.canModifyBasicInfo()) {
            throw new CustomException(ErrorCode.FESTIVAL_INVALID_STATUS);
        }
        validateSameYear(period);

        this.name = name;
        this.description = description;
        this.address = address;
        this.detailAddress = detailAddress;
        this.period = period;
        this.operationTime = operationTime;
    }

    /**
     * 상세주소 없이 축제 기본 정보를 수정한다.
     */
    public void updateBasicInfo(
            FestivalName name,
            FestivalDescription description,
            FestivalAddress address,
            FestivalPeriod period,
            FestivalOperationTime operationTime
    ) {
        updateBasicInfo(
                name,
                description,
                address,
                FestivalDetailAddress.of(null),
                period,
                operationTime
        );
    }

    private void validateSameYear(FestivalPeriod period) {
        if (year != period.getStartDate().getYear()) {
            throw new CustomException(ErrorCode.FESTIVAL_YEAR_CANNOT_BE_CHANGED);
        }
    }

    public String getNameValue() {
        return name.getValue();
    }

    public String getDescriptionValue() {
        return description.getValue();
    }

    public String getAddressValue() {
        return address.getValue();
    }

    public String getDetailAddressValue() {
        return detailAddress == null ? null : detailAddress.getValue();
    }

    public LocalDate getStartDate() {
        return period.getStartDate();
    }

    public LocalDate getEndDate() {
        return period.getEndDate();
    }

    public LocalTime getOperationStartTime() {
        return operationTime.getStartTime();
    }

    public LocalTime getOperationEndTime() {
        return operationTime.getEndTime();
    }

}
