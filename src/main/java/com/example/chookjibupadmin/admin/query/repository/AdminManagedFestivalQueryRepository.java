package com.example.chookjibupadmin.admin.query.repository;

import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalCondition;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 관리자 개인 관리 축제 조회 저장소 계약이다.
 */
public interface AdminManagedFestivalQueryRepository {

    /**
     * 관리자가 현재 관리 중인 축제 목록을 조건으로 조회한다.
     */
    List<AdminManagedFestivalView> searchCurrentManagedFestivals(
            Long adminAccountId,
            AdminManagedFestivalCondition condition
    );

    /**
     * 관리자가 현재 관리 중인 축제를 외부 UUID로 조회한다.
     */
    Optional<AdminManagedFestivalView> findCurrentManagedFestival(
            Long adminAccountId,
            UUID festivalId
    );
}
