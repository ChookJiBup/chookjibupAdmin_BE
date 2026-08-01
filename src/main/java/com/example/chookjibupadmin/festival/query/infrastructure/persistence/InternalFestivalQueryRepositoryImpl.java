package com.example.chookjibupadmin.festival.query.infrastructure.persistence;

import com.example.chookjibupadmin.festival.command.domain.QFestival;
import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSearchCondition;
import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSummaryProjection;
import com.example.chookjibupadmin.festival.query.repository.InternalFestivalQueryRepository;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 사용자 서버용 축제 목록을 DTO projection으로 조회한다.
 */
@Repository
@RequiredArgsConstructor
public class InternalFestivalQueryRepositoryImpl
        implements InternalFestivalQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<InternalFestivalSummaryProjection> searchFestivals(
            InternalFestivalSearchCondition condition,
            Pageable pageable
    ) {
        QFestival festival = QFestival.festival;

        var content = queryFactory
                .select(Projections.constructor(
                        InternalFestivalSummaryProjection.class,
                        festival.publicId,
                        festival.seriesPublicId,
                        festival.name.value,
                        festival.description.value,
                        festival.address.value,
                        festival.detailAddress.value,
                        festival.year,
                        festival.period.startDate,
                        festival.period.endDate,
                        festival.operationTime.startTime,
                        festival.operationTime.endTime
                ))
                .from(festival)
                .where(
                        progressStatusEq(festival, condition),
                        keywordContains(festival, condition)
                )
                .orderBy(festival.period.startDate.asc(), festival.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(festival.id.count())
                .from(festival)
                .where(
                        progressStatusEq(festival, condition),
                        keywordContains(festival, condition)
                )
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total == null ? 0L : total
        );
    }

    private BooleanExpression progressStatusEq(
            QFestival festival,
            InternalFestivalSearchCondition condition
    ) {
        FestivalProgressStatus status = condition.progressStatus();
        if (status == null) {
            return null;
        }

        if (status == FestivalProgressStatus.UPCOMING) {
            return festival.period.startDate.gt(condition.today());
        }
        if (status == FestivalProgressStatus.ONGOING) {
            return festival.period.startDate.loe(condition.today())
                    .and(festival.period.endDate.goe(condition.today()));
        }

        return festival.period.endDate.lt(condition.today());
    }

    private BooleanExpression keywordContains(
            QFestival festival,
            InternalFestivalSearchCondition condition
    ) {
        if (condition.keyword() == null) {
            return null;
        }

        return festival.name.value.containsIgnoreCase(condition.keyword())
                .or(festival.address.value.containsIgnoreCase(condition.keyword()))
                .or(festival.detailAddress.value.containsIgnoreCase(condition.keyword()));
    }
}
