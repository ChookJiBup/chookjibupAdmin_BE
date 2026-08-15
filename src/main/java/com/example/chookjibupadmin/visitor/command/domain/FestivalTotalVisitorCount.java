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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제 전체 방문 인원 수를 저장하는 Aggregate이다.
 */
@Entity
@Getter
@Table(
        name = "festival_visitor_total",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_visitor_total_festival",
                        columnNames = "festival_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalTotalVisitorCount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visitor_total_id")
    private Long id;

    @Column(name = "festival_id", nullable = false, updatable = false)
    private Long festivalId;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "total_visitor_count", nullable = false)
    )
    private VisitorCount visitorCount;

    private FestivalTotalVisitorCount(
            Long festivalId,
            VisitorCount visitorCount
    ) {
        validate(festivalId, visitorCount);
        this.festivalId = festivalId;
        this.visitorCount = visitorCount;
    }

    public static FestivalTotalVisitorCount create(
            Long festivalId,
            VisitorCount visitorCount
    ) {
        return new FestivalTotalVisitorCount(
                festivalId,
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
            VisitorCount visitorCount
    ) {
        if (festivalId == null || visitorCount == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
