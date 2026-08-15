package com.example.chookjibupadmin.visitor.command.domain;

import com.example.chookjibupadmin.common.domain.BaseTimeEntity;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.visitor.command.domain.vo.VisitorCount;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제 일자별 방문 인원 수를 저장하는 Aggregate이다.
 */
@Entity
@Getter
@Table(
        name = "festival_visitor_count",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_visitor_count_festival_date",
                        columnNames = {"festival_id", "visit_date"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalDailyVisitorCount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visitor_count_id")
    private Long id;

    @Column(name = "festival_id", nullable = false, updatable = false)
    private Long festivalId;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "visitor_count", nullable = false)
    )
    private VisitorCount visitorCount;

    private FestivalDailyVisitorCount(
            Long festivalId,
            LocalDate visitDate,
            VisitorCount visitorCount
    ) {
        validate(festivalId, visitDate, visitorCount);
        this.festivalId = festivalId;
        this.visitDate = visitDate;
        this.visitorCount = visitorCount;
    }

    public static FestivalDailyVisitorCount create(
            Long festivalId,
            LocalDate visitDate,
            VisitorCount visitorCount
    ) {
        return new FestivalDailyVisitorCount(
                festivalId,
                visitDate,
                visitorCount
        );
    }

    public void changeVisitorCount(VisitorCount visitorCount) {
        if (visitorCount == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.visitorCount = visitorCount;
    }

    public int getVisitorCountValue() {
        return visitorCount.getValue();
    }

    private void validate(
            Long festivalId,
            LocalDate visitDate,
            VisitorCount visitorCount
    ) {
        if (festivalId == null || visitDate == null || visitorCount == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
