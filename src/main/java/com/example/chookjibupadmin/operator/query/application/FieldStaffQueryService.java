package com.example.chookjibupadmin.operator.query.application;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.query.application.dto.FieldStaffView;
import com.example.chookjibupadmin.operator.query.repository.FieldStaffQueryRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현장 스태프 계정 조회 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FieldStaffQueryService {

    private final FieldStaffQueryRepository queryRepository;
    private final FieldStaffSearchMatcher searchMatcher =
            new FieldStaffSearchMatcher();

    /**
     * 지정한 축제의 활성 현장 스태프 계정 목록을 검색한다.
     */
    public List<FieldStaffView> searchByFestivalId(
            Long festivalId,
            String keyword
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return queryRepository.findAllByFestivalId(festivalId)
                .stream()
                .filter(view -> searchMatcher.matches(view, normalizedKeyword))
                .toList();
    }

    /**
     * 지정한 축제의 활성 현장 스태프 계정을 외부 UUID로 조회한다.
     */
    public FieldStaffView getByFestivalIdAndPublicId(
            Long festivalId,
            UUID publicId
    ) {
        return queryRepository.findByFestivalIdAndPublicId(festivalId, publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.FIELD_STAFF_NOT_FOUND));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim().toLowerCase();
    }
}
