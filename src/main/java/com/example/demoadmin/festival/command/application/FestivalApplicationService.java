package com.example.demoadmin.festival.command.application;

import com.example.demoadmin.admin.command.domain.AdminAccount;
import com.example.demoadmin.admin.command.application.AdminAccountService;
import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.festival.command.application.dto.CreateFestivalCommand;
import com.example.demoadmin.festival.command.application.dto.UpdateFestivalCommand;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.festival.command.domain.FestivalSeries;
import com.example.demoadmin.festival.command.domain.vo.FestivalAddress;
import com.example.demoadmin.festival.command.domain.vo.FestivalDescription;
import com.example.demoadmin.festival.command.domain.vo.FestivalName;
import com.example.demoadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.demoadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 기본 정보 생성과 수정 유스케이스를 조정한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FestivalApplicationService {

    private final FestivalService festivalService;
    private final FestivalSeriesService festivalSeriesService;
    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;

    /**
     * 축제 묶음을 연결한 뒤 연도별 축제 기본 정보를 저장하고 생성자를 1관리자로 배정한다.
     */
    public Festival create(
            CreateFestivalCommand command,
            AdminPrincipal principal
    ) {
        AdminAccount creator = findAuthenticatedAdmin(principal);
        FestivalName name = FestivalName.of(command.name());
        FestivalPeriod period = FestivalPeriod.of(
                command.startDate(),
                command.endDate()
        );
        FestivalSeries series = findOrCreateSeries(command.seriesId(), name);
        validateUniqueFestivalYear(series.getId(), period.getStartDate().getYear());

        Festival festival = Festival.create(
                series.getId(),
                series.getPublicId(),
                name,
                FestivalDescription.of(command.description()),
                FestivalAddress.of(command.address()),
                period,
                FestivalOperationTime.of(
                        command.operationStartTime(),
                        command.operationEndTime()
                )
        );

        Festival savedFestival = festivalService.save(festival);
        if (adminFestivalRoleService == null) {
            creator.assignFestivalOwner(savedFestival.getId());
        } else {
            adminFestivalRoleService.assignFestivalOwner(
                    creator.getId(),
                    savedFestival.getId()
            );
        }

        return savedFestival;
    }

    private FestivalSeries findOrCreateSeries(
            UUID seriesId,
            FestivalName name
    ) {
        if (seriesId != null) {
            return festivalSeriesService.getByPublicId(seriesId);
        }

        String normalizedName = FestivalSeries.normalize(name);
        return festivalSeriesService.findByNormalizedName(normalizedName)
                .orElseGet(() -> festivalSeriesService.save(
                        FestivalSeries.create(name)
                ));
    }

    private void validateUniqueFestivalYear(
            Long seriesId,
            int year
    ) {
        if (festivalService.existsBySeriesIdAndYear(seriesId, year)) {
            throw new CustomException(ErrorCode.FESTIVAL_YEAR_ALREADY_EXISTS);
        }
    }

    /**
     * 1관리자 권한으로 담당 축제의 기본 정보를 수정한다.
     */
    public Festival update(
            UUID festivalId,
            UpdateFestivalCommand command,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalId);
        validateFestivalOwner(festival, adminAccount);
        festival.updateBasicInfo(
                FestivalName.of(command.name()),
                FestivalDescription.of(command.description()),
                FestivalAddress.of(command.address()),
                FestivalPeriod.of(command.startDate(), command.endDate()),
                FestivalOperationTime.of(
                        command.operationStartTime(),
                        command.operationEndTime()
                )
        );

        return festival;
    }

    private void validateFestivalOwner(
            Festival festival,
            AdminAccount adminAccount
    ) {
        if (adminFestivalRoleService == null) {
            if (!festival.getId().equals(adminAccount.getFestivalId())
                    || !adminAccount.canModifyFestivalInfo()) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
            return;
        }

        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(
                        adminAccount.getId(),
                        festival.getId()
                );
        if (!role.canModifyFestivalInfo()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return adminAccountService.getById(principal.adminId());
    }
}
