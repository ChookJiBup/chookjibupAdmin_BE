package com.example.chookjibupadmin.map.command.domain;

/**
 * 축제 배치도 파일의 저장 상태를 표현한다.
 */
public enum FestivalMapStorageStatus {
    UPLOADED,
    UPLOAD_FAILED,
    REPLACED,
    DELETING,
    DELETED
}
