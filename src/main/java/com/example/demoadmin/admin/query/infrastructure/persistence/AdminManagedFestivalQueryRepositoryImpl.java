package com.example.demoadmin.admin.query.infrastructure.persistence;

import com.example.demoadmin.admin.command.domain.AdminStatus;
import com.example.demoadmin.admin.command.domain.QAdminAccount;
import com.example.demoadmin.admin.command.domain.QAdminFestivalRole;
import com.example.demoadmin.admin.query.application.dto.AdminManagedFestivalCondition;
import com.example.demoadmin.admin.query.application.dto.AdminManagedFestivalView;
import com.example.demoadmin.admin.query.repository.AdminManagedFestivalQueryRepository;
import com.example.demoadmin.festival.command.domain.QFestival;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
            AdminManagedFestivalCondition condition
    ) {
        QAdminAccount adminAccount = QAdminAccount.adminAccount;
        QAdminFestivalRole adminFestivalRole = QAdminFestivalRole.adminFestivalRole;
        QFestival festival = QFestival.festival;

        return queryFactory
                .select(Projections.constructor(
                        AdminManagedFestivalView.class,
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
                        keywordContains(festival, condition)
                )
                .orderBy(festival.year.desc(), festival.id.desc())
                .fetch();
    }

    @Override
    public Optional<AdminManagedFestivalView> findCurrentManagedFestival(
            Long adminAccountId,
            UUID festivalId
    ) {
        QAdminAccount adminAccount = QAdminAccount.adminAccount;
        QAdminFestivalRole adminFestivalRole = QAdminFestivalRole.adminFestivalRole;
        QFestival festival = QFestival.festival;

        AdminManagedFestivalView result = queryFactory
                .select(Projections.constructor(
                        AdminManagedFestivalView.class,
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

        return Optional.ofNullable(result);
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
                .or(festival.detailAddress.value.containsIgnoreCase(condition.keyword()));
    }
}
