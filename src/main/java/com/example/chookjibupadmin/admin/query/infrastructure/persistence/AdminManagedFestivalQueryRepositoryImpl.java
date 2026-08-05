package com.example.chookjibupadmin.admin.query.infrastructure.persistence;

import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import com.example.chookjibupadmin.admin.command.domain.QAdminAccount;
import com.example.chookjibupadmin.admin.command.domain.QAdminFestivalRole;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalCondition;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalProjection;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalView;
import com.example.chookjibupadmin.admin.query.repository.AdminManagedFestivalQueryRepository;
import com.example.chookjibupadmin.festival.command.domain.QFestival;
import com.example.chookjibupadmin.festival.location.domain.QFestivalLocation;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminManagedFestivalQueryRepositoryImpl
        implements AdminManagedFestivalQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdminManagedFestivalView> searchCurrentManagedFestivals(
            Long adminAccountId,
            AdminManagedFestivalCondition condition,
            LocalDate today
    ) {
        QAdminAccount adminAccount = QAdminAccount.adminAccount;
        QAdminFestivalRole adminFestivalRole = QAdminFestivalRole.adminFestivalRole;
        QFestival festival = QFestival.festival;

        List<AdminManagedFestivalProjection> projections = queryFactory
                .select(Projections.constructor(
                        AdminManagedFestivalProjection.class,
                        festival.publicId,
                        festival.name.value,
                        festival.year,
                        adminFestivalRole.role,
                        festival.status,
                        festival.address.value,
                        festival.detailAddress.value,
                        festival.period.startDate,
                        festival.period.endDate
                ))
                .from(adminFestivalRole)
                .join(adminAccount).on(adminAccount.id.eq(adminFestivalRole.adminAccountId))
                .join(festival).on(festival.id.eq(adminFestivalRole.festivalId))
                .where(
                        adminFestivalRole.adminAccountId.eq(adminAccountId),
                        adminAccount.status.eq(AdminStatus.ACTIVE),
                        roleEq(adminFestivalRole, condition),
                        yearEq(festival, condition),
                        keywordContains(festival, condition),
                        progressStatusEq(festival, condition, today)
                )
                .orderBy(progressStatusOrder(festival, today))
                .fetch();

        return projections.stream()
                .map(projection -> projection.toView(today))
                .toList();
    }

    @Override
    public Optional<AdminManagedFestivalView> findCurrentManagedFestival(
            Long adminAccountId,
            UUID festivalId,
            LocalDate today
    ) {
        QAdminAccount adminAccount = QAdminAccount.adminAccount;
        QAdminFestivalRole adminFestivalRole = QAdminFestivalRole.adminFestivalRole;
        QFestival festival = QFestival.festival;

        AdminManagedFestivalProjection result = queryFactory
                .select(Projections.constructor(
                        AdminManagedFestivalProjection.class,
                        festival.publicId,
                        festival.name.value,
                        festival.year,
                        adminFestivalRole.role,
                        festival.status,
                        festival.address.value,
                        festival.detailAddress.value,
                        festival.period.startDate,
                        festival.period.endDate
                ))
                .from(adminFestivalRole)
                .join(adminAccount).on(adminAccount.id.eq(adminFestivalRole.adminAccountId))
                .join(festival).on(festival.id.eq(adminFestivalRole.festivalId))
                .where(
                        adminFestivalRole.adminAccountId.eq(adminAccountId),
                        adminAccount.status.eq(AdminStatus.ACTIVE),
                        festival.publicId.eq(festivalId)
                )
                .fetchOne();

        return Optional.ofNullable(result)
                .map(projection -> projection.toView(today));
    }

    private BooleanExpression roleEq(
            QAdminFestivalRole adminFestivalRole,
            AdminManagedFestivalCondition condition
    ) {
        if (condition.role() == null) {
            return null;
        }

        return adminFestivalRole.role.eq(condition.role());
    }

    private BooleanExpression yearEq(
            QFestival festival,
            AdminManagedFestivalCondition condition
    ) {
        if (condition.year() == null) {
            return null;
        }

        return festival.year.eq(condition.year());
    }

    private BooleanExpression keywordContains(
            QFestival festival,
            AdminManagedFestivalCondition condition
    ) {
        if (condition.keyword() == null) {
            return null;
        }

        return festival.name.value.containsIgnoreCase(condition.keyword())
                .or(festival.address.value.containsIgnoreCase(condition.keyword()))
                .or(festival.detailAddress.value.containsIgnoreCase(condition.keyword()))
                .or(locationContains(festival, condition.keyword()));
    }

    private BooleanExpression locationContains(QFestival festival, String keyword) {
        QFestivalLocation location = new QFestivalLocation("managedFestivalLocation");
        BooleanExpression locationMatches = location.locationName
                .containsIgnoreCase(keyword)
                .or(location.roadAddress.containsIgnoreCase(keyword))
                .or(location.jibunAddress.containsIgnoreCase(keyword))
                .or(location.detailAddress.containsIgnoreCase(keyword));

        return JPAExpressions.selectOne()
                .from(location)
                .where(location.festival.id.eq(festival.id).and(locationMatches))
                .exists();
    }

    private BooleanExpression progressStatusEq(
            QFestival festival,
            AdminManagedFestivalCondition condition,
            LocalDate today
    ) {
        FestivalProgressStatus status = condition.progressStatus();
        if (status == null) {
            return null;
        }
        if (status == FestivalProgressStatus.UPCOMING) {
            return festival.period.startDate.gt(today);
        }
        if (status == FestivalProgressStatus.ONGOING) {
            return festival.period.startDate.loe(today)
                    .and(festival.period.endDate.goe(today));
        }
        return festival.period.endDate.lt(today);
    }

    private OrderSpecifier<?>[] progressStatusOrder(
            QFestival festival,
            LocalDate today
    ) {
        BooleanExpression upcoming = festival.period.startDate.gt(today);
        BooleanExpression ongoing = festival.period.startDate.loe(today)
                .and(festival.period.endDate.goe(today));
        BooleanExpression completed = festival.period.endDate.lt(today);

        NumberExpression<Integer> statusOrder = new CaseBuilder()
                .when(upcoming).then(0)
                .when(ongoing).then(1)
                .otherwise(2);
        DateExpression<LocalDate> upcomingStartOrder = new CaseBuilder()
                .when(upcoming).then(festival.period.startDate)
                .otherwise((LocalDate) null);
        DateExpression<LocalDate> ongoingEndOrder = new CaseBuilder()
                .when(ongoing).then(festival.period.endDate)
                .otherwise((LocalDate) null);
        DateExpression<LocalDate> completedEndOrder = new CaseBuilder()
                .when(completed).then(festival.period.endDate)
                .otherwise((LocalDate) null);

        return new OrderSpecifier<?>[]{
                statusOrder.asc(),
                upcomingStartOrder.asc().nullsLast(),
                ongoingEndOrder.asc().nullsLast(),
                completedEndOrder.desc().nullsLast(),
                festival.id.desc()
        };
    }
}
