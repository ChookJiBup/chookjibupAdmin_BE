package com.example.chookjibupadmin.admin.query.infrastructure.persistence;

import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import com.example.chookjibupadmin.admin.command.domain.QAdminAccount;
import com.example.chookjibupadmin.admin.command.domain.QAdminFestivalRole;
import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminCandidateView;
import com.example.chookjibupadmin.admin.query.repository.AdminSubAdminCandidateQueryRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminSubAdminCandidateQueryRepositoryImpl
        implements AdminSubAdminCandidateQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdminSubAdminCandidateView> searchCandidates(
            Long festivalId,
            String keyword
    ) {
        QAdminAccount adminAccount = QAdminAccount.adminAccount;
        QAdminFestivalRole adminFestivalRole = QAdminFestivalRole.adminFestivalRole;

        return queryFactory
                .select(Projections.constructor(
                        AdminSubAdminCandidateView.class,
                        adminAccount.publicId,
                        adminAccount.email.value,
                        adminAccount.name.value,
                        adminAccount.organization.value,
                        adminAccount.status
                ))
                .from(adminAccount)
                .leftJoin(adminFestivalRole)
                .on(adminFestivalRole.adminAccountId.eq(adminAccount.id)
                        .and(adminFestivalRole.festivalId.eq(festivalId)))
                .where(
                        adminFestivalRole.id.isNull(),
                        adminAccount.status.eq(AdminStatus.ACTIVE),
                        keywordContains(adminAccount, keyword)
                )
                .orderBy(adminAccount.id.asc())
                .fetch();
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
