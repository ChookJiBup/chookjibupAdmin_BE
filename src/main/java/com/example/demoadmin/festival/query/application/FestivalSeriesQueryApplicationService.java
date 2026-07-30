package com.example.demoadmin.festival.query.application;

import com.example.demoadmin.admin.command.application.AdminAccountService;
import com.example.demoadmin.admin.command.domain.AdminAccount;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.festival.query.application.dto.FestivalSeriesSearchView;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 등록용 기존 축제 검색 흐름을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalSeriesQueryApplicationService {

    private static final int DEFAULT_LIMIT = 10;

    private final AdminAccountService adminAccountService;
    private final FestivalSeriesQueryService festivalSeriesQueryService;

    /**
     * 인증 관리자가 등록 화면에서 선택할 기존 축제를 검색한다.
     */
    public List<FestivalSeriesSearchView> search(
            String keyword,
            Integer limit,
            AdminPrincipal principal
    ) {
        validateAuthenticatedAdmin(principal);
        return festivalSeriesQueryService.search(
                keyword,
                limit == null ? DEFAULT_LIMIT : limit
        );
    }

    private void validateAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        AdminAccount adminAccount = adminAccountService.getById(principal.adminId());
        if (!adminAccount.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }
    }
}
