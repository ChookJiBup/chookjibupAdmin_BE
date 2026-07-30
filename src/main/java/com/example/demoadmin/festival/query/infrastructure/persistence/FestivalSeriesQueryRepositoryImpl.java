package com.example.demoadmin.festival.query.infrastructure.persistence;

import com.example.demoadmin.festival.command.domain.QFestival;
import com.example.demoadmin.festival.command.domain.QFestivalSeries;
import com.example.demoadmin.festival.query.application.dto.FestivalSeriesSearchView;
import com.example.demoadmin.festival.query.repository.FestivalSeriesQueryRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 축제 등록용 기존 축제 시리즈를 QueryDSL DTO projection으로 조회한다.
 */
@Repository
@RequiredArgsConstructor
public class FestivalSeriesQueryRepositoryImpl
        implements FestivalSeriesQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<FestivalSeriesSearchView> search(
            String keyword,
            int limit
    ) {
        QFestivalSeries series = QFestivalSeries.festivalSeries;
        QFestival latestFestival = new QFestival("latestFestival");
        QFestival festivalSub = new QFestival("festivalSub");

        return queryFactory
                .select(Projections.constructor(
                        FestivalSeriesSearchView.class,
                        series.publicId,
                        series.name.value,
                        latestFestival.publicId,
                        latestFestival.year,
                        latestFestival.description.value,
                        latestFestival.address.value,
                        latestFestival.detailAddress.value,
                        latestFestival.period.startDate,
                        latestFestival.period.endDate,
                        latestFestival.operationTime.startTime,
                        latestFestival.operationTime.endTime
                ))
                .from(series)
                .leftJoin(latestFestival)
                .on(
                        latestFestival.seriesId.eq(series.id),
                        latestFestival.year.eq(
                                JPAExpressions
                                        .select(festivalSub.year.max())
                                        .from(festivalSub)
                                        .where(festivalSub.seriesId.eq(series.id))
                        )
                )
                .where(series.name.value.containsIgnoreCase(keyword))
                .orderBy(series.name.value.asc(), series.id.asc())
                .limit(limit)
                .fetch();
    }
}
