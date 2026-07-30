package com.example.demoadmin.booth.command.application;

import com.example.demoadmin.booth.command.domain.BoothQueueLine;
import com.example.demoadmin.booth.command.domain.BoothQueueLineRepository;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부스 대기 라인 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoothQueueLineService {

    private final BoothQueueLineRepository boothQueueLineRepository;

    @Transactional
    public BoothQueueLine save(BoothQueueLine queueLine) {
        return boothQueueLineRepository.save(queueLine);
    }

    public BoothQueueLine getByBoothIdAndPublicId(
            Long boothId,
            UUID publicId
    ) {
        return boothQueueLineRepository.findByBoothIdAndPublicId(boothId, publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOTH_QUEUE_LINE_NOT_FOUND));
    }

    public boolean existsByBoothIdAndLineOrder(
            Long boothId,
            int lineOrder
    ) {
        return boothQueueLineRepository.existsByBoothIdAndLineOrder(boothId, lineOrder);
    }

    public boolean existsByBoothIdAndLineOrderAndIdNot(
            Long boothId,
            int lineOrder,
            Long id
    ) {
        return boothQueueLineRepository.existsByBoothIdAndLineOrderAndIdNot(
                boothId,
                lineOrder,
                id
        );
    }
}
