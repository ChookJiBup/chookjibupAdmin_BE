package com.example.chookjibupadmin.admin.query.application;

import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalCondition;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalView;
import com.example.chookjibupadmin.admin.query.repository.AdminManagedFestivalQueryRepository;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 개인 관리 축제 조회 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminManagedFestivalQueryService {

    private final AdminManagedFestivalQueryRepository queryRepository;

    /**
     * 관리자가 현재 관리 중인 축제 목록을 조건으로 조회한다.
     */
    public List<AdminManagedFestivalView> searchCurrentManagedFestivals(
            Long adminAccountId,
            AdminManagedFestivalCondition condition,
            LocalDate today
    ) {
        return queryRepository.searchCurrentManagedFestivals(
                adminAccountId,
                condition.normalize(),
                today
        );
    }

    /**
     * 관리자가 현재 관리 중인 축제를 외부 UUID로 조회한다.
     */
    public AdminManagedFestivalView getCurrentManagedFestival(
            Long adminAccountId,
            UUID festivalId,
            LocalDate today
    ) {
        return queryRepository.findCurrentManagedFestival(
                        adminAccountId,
                        festivalId,
                        today
                )
                .orElseThrow(() -> new CustomException(ErrorCode.FESTIVAL_NOT_FOUND));
    }
}
