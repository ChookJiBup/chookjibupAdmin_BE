package com.example.chookjibupadmin.admin.query.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalDetail;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalView;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.festival.location.application.dto.FestivalLocationDetail;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관리자가 담당하는 축제의 상세 수정 화면 데이터를 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminManagedFestivalDetailQueryApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminManagedFestivalQueryService managedFestivalQueryService;
    private final FestivalService festivalService;
    private final FestivalLocationService festivalLocationService;
    private final Clock clock;

    public AdminManagedFestivalDetail getManagedFestival(
            UUID festivalId,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        AdminManagedFestivalView managedFestival = managedFestivalQueryService
                .getCurrentManagedFestival(
                        adminAccount.getId(),
                        festivalId,
                        LocalDate.now(clock)
                );
        Festival festival = festivalService.getByPublicId(festivalId);

        return new AdminManagedFestivalDetail(
                festival.getPublicId(),
                festival.getSeriesPublicId(),
                festival.getNameValue(),
                festival.getDescriptionValue(),
                festival.getYear(),
                managedFestival.role(),
                festival.getStatus(),
                managedFestival.progressStatus(),
                festival.getAddressValue(),
                festival.getDetailAddressValue(),
                festival.getStartDate(),
                festival.getEndDate(),
                festival.getOperationStartTime(),
                festival.getOperationEndTime(),
                festivalLocationService.findAllByFestivalId(festival.getId())
                        .stream()
                        .map(FestivalLocationDetail::from)
                        .toList()
        );
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        AdminAccount adminAccount = adminAccountService.getById(principal.adminId());
        if (!adminAccount.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }
        return adminAccount;
    }
}
