package com.example.chookjibupadmin.admin.command.application;

import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRoleRepository;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제별 관리자 역할 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminFestivalRoleService {

    /** 잠금 대기로 요청이 무한정 매달리지 않도록 역할 쓰기 트랜잭션에 두는 상한(초)이다. */
    private static final int ROLE_WRITE_TIMEOUT_SECONDS = 10;

    private final AdminFestivalRoleRepository adminFestivalRoleRepository;

    @Transactional
    public AdminFestivalRole save(AdminFestivalRole adminFestivalRole) {
        return adminFestivalRoleRepository.save(adminFestivalRole);
    }

    @Transactional
    public AdminFestivalRole assignFestivalOwner(
            Long adminAccountId,
            Long festivalId
    ) {
        if (adminFestivalRoleRepository.existsByFestivalIdAndRole(
                festivalId,
                AdminRole.FESTIVAL_OWNER
        )) {
            throw new CustomException(ErrorCode.AUTH_FESTIVAL_OWNER_ALREADY_EXISTS);
        }
        if (adminFestivalRoleRepository.existsByAdminAccountIdAndFestivalId(
                adminAccountId,
                festivalId
        )) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_ALREADY_ASSIGNED);
        }

        return save(AdminFestivalRole.createFestivalOwner(
                adminAccountId,
                festivalId
        ));
    }

    /**
     * 제2관리자 역할을 부여한다.
     *
     * <p>같은 제1관리자가 같은 대상을 다시 배정하면 이미 저장된 역할을 그대로 돌려준다.
     * 배정이 반영됐는데도 재시도가 실패로 응답되어 화면에 「추가에 실패했습니다」가 뜨는
     * 상황을 막기 위한 멱등 처리다.
     */
    @Transactional(timeout = ROLE_WRITE_TIMEOUT_SECONDS)
    public AdminFestivalRole assignSubAdmin(
            Long adminAccountId,
            Long festivalId,
            Long invitedByAdminId
    ) {
        return adminFestivalRoleRepository
                .findByAdminAccountIdAndFestivalId(adminAccountId, festivalId)
                .map(existing -> reuseSubAdminRole(existing, invitedByAdminId))
                .orElseGet(() -> save(AdminFestivalRole.createSubAdmin(
                        adminAccountId,
                        festivalId,
                        invitedByAdminId
                )));
    }

    /**
     * 유니크 제약에 걸린 뒤 이미 저장된 제2관리자 역할을 다시 읽는다.
     *
     * <p>동시에 들어온 같은 배정 요청 하나가 먼저 커밋하면 나머지는 제약 위반으로 실패한다.
     * 이때 저장된 결과를 확인해 성공으로 응답하기 위해 사용한다.
     */
    public AdminFestivalRole getAssignedSubAdmin(
            Long adminAccountId,
            Long festivalId,
            Long invitedByAdminId
    ) {
        return adminFestivalRoleRepository
                .findByAdminAccountIdAndFestivalId(adminAccountId, festivalId)
                .map(existing -> reuseSubAdminRole(existing, invitedByAdminId))
                .orElseThrow(() -> new CustomException(
                        ErrorCode.AUTH_ADMIN_ALREADY_ASSIGNED
                ));
    }

    /**
     * 이미 존재하는 역할이 같은 제1관리자가 부여한 제2관리자 역할인지 확인한다.
     */
    private AdminFestivalRole reuseSubAdminRole(
            AdminFestivalRole existing,
            Long invitedByAdminId
    ) {
        if (existing.getRole() != AdminRole.SUB_ADMIN
                || !Objects.equals(existing.getInvitedByAdminId(), invitedByAdminId)) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_ALREADY_ASSIGNED);
        }
        return existing;
    }

    public AdminFestivalRole getByAdminAccountIdAndFestivalId(
            Long adminAccountId,
            Long festivalId
    ) {
        return adminFestivalRoleRepository
                .findByAdminAccountIdAndFestivalId(adminAccountId, festivalId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));
    }

    /**
     * 관리자 계정 목록이 특정 축제에서 갖는 역할을 잠금 조회한다.
     */
    public List<AdminFestivalRole> getAllByAdminAccountIdsAndFestivalId(
            Collection<Long> adminAccountIds,
            Long festivalId
    ) {
        return adminFestivalRoleRepository
                .findAllByAdminAccountIdInAndFestivalId(
                        adminAccountIds,
                        festivalId
                );
    }

    /**
     * 축제에 연결된 모든 관리자 역할을 조회한다.
     */
    public List<AdminFestivalRole> getAllByFestivalId(Long festivalId) {
        return adminFestivalRoleRepository.findAllByFestivalId(festivalId);
    }

    /**
     * 관리자가 어떤 축제에서든 총괄 역할을 갖고 있는지 확인한다.
     */
    public boolean hasFestivalOwnerRole(Long adminAccountId) {
        return adminFestivalRoleRepository.existsByAdminAccountIdAndRole(
                adminAccountId,
                AdminRole.FESTIVAL_OWNER
        );
    }

    /**
     * 축제 관리자 역할 관계를 일괄 삭제한다.
     */
    @Transactional
    public void deleteAll(Collection<AdminFestivalRole> roles) {
        adminFestivalRoleRepository.deleteAll(roles);
    }
}
