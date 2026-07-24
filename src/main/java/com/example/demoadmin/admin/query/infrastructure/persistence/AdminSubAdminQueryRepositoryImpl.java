package com.example.demoadmin.admin.query.infrastructure.persistence;

import com.example.demoadmin.admin.command.domain.AdminRole;
import com.example.demoadmin.admin.command.domain.AdminStatus;
import com.example.demoadmin.admin.command.domain.QAdminAccount;
import com.example.demoadmin.admin.command.domain.QAdminFestivalRole;
import com.example.demoadmin.admin.query.application.dto.AdminSubAdminView;
import com.example.demoadmin.admin.query.repository.AdminSubAdminQueryRepository;
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
public class AdminSubAdminQueryRepositoryImpl
        implements AdminSubAdminQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdminSubAdminView> searchInvitedSubAdmins(
            Long festivalId,
            Long invitedByAdminId,
            String keyword
    ) {
        QAdminAccount adminAccount = QAdminAccount.adminAccount;
        QAdminFestivalRole adminFestivalRole = QAdminFestivalRole.adminFestivalRole;

        return queryFactory
                .select(Projections.constructor(
                        AdminSubAdminView.class,
                        adminAccount.publicId,
                        adminAccount.email.value,
                        adminAccount.name.value,
                        adminAccount.organization.value,
                        adminAccount.status
                ))
                .from(adminFestivalRole)
                .join(adminAccount).on(adminAccount.id.eq(adminFestivalRole.adminAccountId))
                .where(
                        adminFestivalRole.festivalId.eq(festivalId),
                        adminFestivalRole.invitedByAdminId.eq(invitedByAdminId),
                        adminFestivalRole.role.eq(AdminRole.SUB_ADMIN),
                        adminAccount.status.eq(AdminStatus.ACTIVE),
                        keywordContains(adminAccount, keyword)
                )
                .orderBy(adminAccount.id.asc())
                .fetch();
    }

    @Override
    public Optional<AdminSubAdminView> findInvitedSubAdmin(
            Long festivalId,
            Long invitedByAdminId,
            UUID publicId
    ) {
        QAdminAccount adminAccount = QAdminAccount.adminAccount;
        QAdminFestivalRole adminFestivalRole = QAdminFestivalRole.adminFestivalRole;

        AdminSubAdminView result = queryFactory
                .select(Projections.constructor(
                        AdminSubAdminView.class,
                        adminAccount.publicId,
                        adminAccount.email.value,
                        adminAccount.name.value,
                        adminAccount.organization.value,
                        adminAccount.status
                ))
                .from(adminFestivalRole)
                .join(adminAccount).on(adminAccount.id.eq(adminFestivalRole.adminAccountId))
                .where(
                        adminFestivalRole.festivalId.eq(festivalId),
                        adminFestivalRole.invitedByAdminId.eq(invitedByAdminId),
                        adminAccount.publicId.eq(publicId),
                        adminFestivalRole.role.eq(AdminRole.SUB_ADMIN),
                        adminAccount.status.eq(AdminStatus.ACTIVE)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    private BooleanExpression keywordContains(
            QAdminAccount adminAccount,
            String keyword
    ) {
        if (keyword == null) {
            return null;
        }

        return adminAccount.email.value.containsIgnoreCase(keyword)
                .or(adminAccount.name.value.containsIgnoreCase(keyword))
                .or(adminAccount.organization.value.containsIgnoreCase(keyword));
    }
}
