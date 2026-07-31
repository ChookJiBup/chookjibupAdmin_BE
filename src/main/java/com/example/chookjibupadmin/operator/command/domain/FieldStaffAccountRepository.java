package com.example.chookjibupadmin.operator.command.domain;

import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import java.util.Optional;
import java.util.UUID;

/**
 * 현장 스태프 계정을 조회하고 저장하는 저장소 계약이다.
 */
public interface FieldStaffAccountRepository {

    /**
     * 현장 스태프 계정을 저장한다.
     */
    FieldStaffAccount save(FieldStaffAccount fieldStaffAccount);

    /**
     * 내부 식별자로 현장 스태프 계정을 조회한다.
     */
    Optional<FieldStaffAccount> findById(Long fieldStaffAccountId);

    /**
     * 외부 노출용 UUID로 현장 스태프 계정을 조회한다.
     */
    Optional<FieldStaffAccount> findByPublicId(UUID publicId);

    /**
     * 축제와 로그인 아이디로 현장 스태프 계정을 조회한다.
     */
    Optional<FieldStaffAccount> findByFestivalIdAndLoginId(
            Long festivalId,
            FieldStaffLoginId loginId
    );

    /**
     * 축제 안에서 같은 로그인 아이디가 이미 사용 중인지 확인한다.
     */
    boolean existsByFestivalIdAndLoginId(
            Long festivalId,
            FieldStaffLoginId loginId
    );
}
