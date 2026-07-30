package com.example.demoadmin.booth.query.infrastructure.persistence;

import com.example.demoadmin.booth.command.domain.QBoothQueueLine;
import com.example.demoadmin.booth.command.domain.QFestivalBooth;
import com.example.demoadmin.booth.query.application.dto.BoothQueueLineView;
import com.example.demoadmin.booth.query.application.dto.BoothView;
import com.example.demoadmin.booth.query.repository.BoothQueryRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BoothQueryRepositoryImpl implements BoothQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<BoothView> findAllByFestivalId(Long festivalId) {
        QFestivalBooth booth = QFestivalBooth.festivalBooth;
        QBoothQueueLine queueLine = QBoothQueueLine.boothQueueLine;

        return queryFactory
                .select(Projections.constructor(
                        BoothView.class,
                        booth.publicId,
                        booth.name.value,
                        booth.category.value,
                        booth.location.value,
                        booth.description.value,
                        booth.operatingStatus,
                        queueLine.publicId,
                        queueLine.lineOrder,
                        queueLine.label.value,
                        booth.expectedWaitingMinutes
                ))
                .from(booth)
                .leftJoin(queueLine).on(queueLine.id.eq(booth.currentQueueLineId))
                .where(booth.festivalId.eq(festivalId))
                .orderBy(booth.id.asc())
                .fetch();
    }

    @Override
    public Optional<BoothView> findByFestivalIdAndPublicId(
            Long festivalId,
            UUID boothId
    ) {
        QFestivalBooth booth = QFestivalBooth.festivalBooth;
        QBoothQueueLine queueLine = QBoothQueueLine.boothQueueLine;

        BoothView result = queryFactory
                .select(Projections.constructor(
                        BoothView.class,
                        booth.publicId,
                        booth.name.value,
                        booth.category.value,
                        booth.location.value,
                        booth.description.value,
                        booth.operatingStatus,
                        queueLine.publicId,
                        queueLine.lineOrder,
                        queueLine.label.value,
                        booth.expectedWaitingMinutes
                ))
                .from(booth)
                .leftJoin(queueLine).on(queueLine.id.eq(booth.currentQueueLineId))
                .where(
                        booth.festivalId.eq(festivalId),
                        booth.publicId.eq(boothId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<BoothQueueLineView> findQueueLinesByFestivalIdAndBoothPublicId(
            Long festivalId,
            UUID boothId
    ) {
        QFestivalBooth booth = QFestivalBooth.festivalBooth;
        QBoothQueueLine queueLine = QBoothQueueLine.boothQueueLine;

        return queryFactory
                .select(Projections.constructor(
                        BoothQueueLineView.class,
                        queueLine.publicId,
                        queueLine.lineOrder,
                        queueLine.label.value,
                        queueLine.expectedWaitingMinutes,
                        queueLine.maxCapacity,
                        queueLine.pathData,
                        queueLine.entryPointData
                ))
                .from(booth)
                .join(queueLine).on(queueLine.boothId.eq(booth.id))
                .where(
                        booth.festivalId.eq(festivalId),
                        booth.publicId.eq(boothId)
                )
                .orderBy(queueLine.lineOrder.asc())
                .fetch();
    }
}
