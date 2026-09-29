package com.example.chookjibupadmin.api.festival;

import com.example.chookjibupadmin.festival.query.application.FestivalImageReadApplicationService;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 화면이 축제 대표 이미지를 읽을 수 있는 공개 진입점을 제공한다.
 */
@RestController
@RequestMapping("/api/public/festivals")
@RequiredArgsConstructor
public class PublicFestivalImageController {

    private final FestivalImageReadApplicationService imageReadService;

    @GetMapping("/{festivalId}/image")
    public ResponseEntity<Void> read(@PathVariable UUID festivalId) {
        URI signedUrl = imageReadService.createReadUrl(festivalId).url();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, signedUrl.toString())
                .build();
    }
}
