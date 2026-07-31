package com.example.chookjibupadmin.operator.query.repository;

import com.example.chookjibupadmin.operator.query.application.dto.FieldStaffView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 현장 스태프 계정 조회 저장소 계약이다.
 */
public interface FieldStaffQueryRepository {

    /**
     * 지정한 축제의 활성 현장 스태프 계정 목록을 조회한다.
     */
    List<FieldStaffView> findAllByFestivalId(Long festivalId);

    /**
     * 지정한 축제의 활성 현장 스태프 계정을 외부 UUID로 조회한다.
     */
    Optional<FieldStaffView> findByFestivalIdAndPublicId(
            Long festivalId,
            UUID publicId
    );
}
