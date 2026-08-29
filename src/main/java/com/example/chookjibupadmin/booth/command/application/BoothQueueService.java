package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.booth.command.domain.BoothQueue;
import com.example.chookjibupadmin.booth.command.domain.BoothQueueRepository;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부스 대기열 저장소 래퍼이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoothQueueService {

    private final BoothQueueRepository boothQueueRepository;

    @Transactional
    public BoothQueue save(BoothQueue queue) {
        return boothQueueRepository.save(queue);
    }

    public BoothQueue getByPublicId(UUID publicId) {
        return boothQueueRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOOTH_QUEUE_NOT_FOUND));
    }

    public Optional<BoothQueue> findByBoothId(Long boothId) {
        return boothQueueRepository.findByBoothId(boothId);
    }

    public List<BoothQueue> findAllByFestivalId(Long festivalId) {
        return boothQueueRepository.findAllByFestivalId(festivalId);
    }

    public List<BoothQueue> findAllByBoothIdIn(Collection<Long> boothIds) {
        return boothQueueRepository.findAllByBoothIdIn(boothIds);
    }
}
