package com.example.chookjibupadmin.map.command.application.port;

import com.example.chookjibupadmin.map.command.application.dto.StoredMapImageFile;
import com.example.chookjibupadmin.map.command.application.dto.MapImageReadUrl;
import java.net.URI;

/**
 * 배치도 이미지 객체 저장소 계약이다.
 */
public interface MapImageStoragePort {

    void upload(StoredMapImageFile imageFile);

    /** 공개 객체의 만료되지 않는 조회 주소를 반환한다. */
    URI createPublicUrl(String objectKey);

    void delete(String objectKey);

    MapImageReadUrl createReadUrl(String objectKey);

    byte[] read(String objectKey, long maxBytes);
}
