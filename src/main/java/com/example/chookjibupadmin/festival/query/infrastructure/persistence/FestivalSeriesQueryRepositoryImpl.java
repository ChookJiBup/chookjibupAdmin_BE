package com.example.chookjibupadmin.festival.query.infrastructure.persistence;

import com.example.chookjibupadmin.festival.command.domain.FestivalSeries;
import com.example.chookjibupadmin.festival.command.domain.QFestival;
import com.example.chookjibupadmin.festival.command.domain.QFestivalSeries;
import com.example.chookjibupadmin.festival.query.application.dto.FestivalSeriesSearchView;
import com.example.chookjibupadmin.festival.query.repository.FestivalSeriesQueryRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 축제 등록용 기존 축제를 QueryDSL DTO projection으로 조회한다.
 *
 * <p>{@code festivals}는 공공데이터 파이프라인이 적재한 축제와 관리자가 직접 등록한
 * 축제를 함께 담는 공유 마스터다. 이 중 {@code festival_series}에 묶이는 것은 관리자
 * 등록분뿐이라, 시리즈만 조회하면 파이프라인 적재 축제가 검색되지 않는다. 그래서
 * 시리즈 검색과 시리즈에 묶이지 않은 축제 검색을 각각 수행한 뒤 축제명 기준으로
 * 합친다.</p>
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
        return merge(
                searchSeries(keyword, limit),
                searchUnlinkedFestivals(keyword, limit),
                limit
        );
    }

    /**
     * 관리자 등록으로 만들어진 축제 시리즈와 각 시리즈의 최근 개최 정보를 조회한다.
     */
    private List<FestivalSeriesSearchView> searchSeries(
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

    /**
     * 아직 시리즈에 묶이지 않은 축제(공공데이터 파이프라인 적재분)를 조회한다.
     *
     * <p>같은 축제가 연도별로 여러 행으로 쌓여 있으므로 축제명당 가장 최근 회차
     * 한 건만 남긴다.</p>
     */
    private List<FestivalSeriesSearchView> searchUnlinkedFestivals(
            String keyword,
            int limit
    ) {
        QFestival festival = new QFestival("unlinkedFestival");
        QFestival newerFestival = new QFestival("newerUnlinkedFestival");

        return queryFactory
                .select(Projections.constructor(
                        FestivalSeriesSearchView.class,
                        // series_id가 비면 series_public_id도 함께 비어 있다.
                        // 아직 시리즈가 없다는 뜻으로 그대로 내보낸다.
                        festival.seriesPublicId,
                        festival.name.value,
                        festival.publicId,
                        festival.year,
                        festival.description.value,
                        festival.address.value,
                        festival.detailAddress.value,
                        festival.period.startDate,
                        festival.period.endDate,
                        festival.operationTime.startTime,
                        festival.operationTime.endTime
                ))
                .from(festival)
                .where(
                        festival.seriesId.isNull(),
                        festival.name.value.containsIgnoreCase(keyword),
                        isLatestAmongSameName(festival, newerFestival)
                )
                .orderBy(festival.name.value.asc(), festival.id.asc())
                .limit(limit)
                .fetch();
    }

    /**
     * 같은 축제명을 가진 다른 미연결 축제 중 더 최근 회차가 없는지 확인한다.
     */
    private BooleanExpression isLatestAmongSameName(
            QFestival festival,
            QFestival newerFestival
    ) {
        return JPAExpressions
                .selectOne()
                .from(newerFestival)
                .where(
                        newerFestival.seriesId.isNull(),
                        normalizedName(newerFestival).eq(normalizedName(festival)),
                        newerFestival.year.gt(festival.year)
                                .or(newerFestival.year.eq(festival.year)
                                        .and(newerFestival.id.gt(festival.id)))
                )
                .notExists();
    }

    /**
     * 축제명 비교용 정규화 식을 만든다. 공백과 대소문자 차이는 무시한다.
     */
    private StringExpression normalizedName(QFestival festival) {
        return Expressions.stringTemplate(
                "lower(replace({0}, ' ', ''))",
                festival.name.value
        );
    }

    /**
     * 시리즈 검색 결과와 미연결 축제 검색 결과를 축제명 기준으로 합친다.
     *
     * <p>같은 축제명이 양쪽에 모두 있으면 시리즈 쪽을 남긴다. 관리자가 이미 등록한
     * 축제라면 파이프라인 적재분이 아니라 등록된 최근 회차를 불러와야 한다.</p>
     */
    private List<FestivalSeriesSearchView> merge(
            List<FestivalSeriesSearchView> seriesViews,
            List<FestivalSeriesSearchView> unlinkedViews,
            int limit
    ) {
        Map<String, FestivalSeriesSearchView> byName = new LinkedHashMap<>();
        Stream.concat(seriesViews.stream(), unlinkedViews.stream())
                .forEach(view -> byName.putIfAbsent(
                        FestivalSeries.normalize(view.name()),
                        view
                ));

        return byName.values()
                .stream()
                .sorted(Comparator.comparing(FestivalSeriesSearchView::name))
                .limit(limit)
                .toList();
    }
}
