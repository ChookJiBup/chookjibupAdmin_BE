package com.example.chookjibupadmin.booth.command.domain;

import com.example.chookjibupadmin.common.domain.BaseTimeEntity;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제에 승인된 부스 마스터이다. 지도 노드 승인 후에만 생성된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "booth_info")
public class BoothInfo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booth_id")
    private Long id;

    @Column(name = "festival_id", nullable = false, updatable = false)
    private Long festivalId;

    @Column(name = "roadmap_node_id", updatable = false)
    private Long roadmapNodeId;

    @Column(name = "booth_name", nullable = false, columnDefinition = "TEXT")
    private String boothName;

    @Column(name = "booth_content", columnDefinition = "TEXT")
    private String boothContent;

    @Column(name = "booth_location", columnDefinition = "TEXT")
    private String boothLocation;

    public static BoothInfo create(
            Long festivalId,
            Long roadmapNodeId,
            String boothName
    ) {
        if (festivalId == null
                || roadmapNodeId == null
                || boothName == null
                || boothName.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        BoothInfo booth = new BoothInfo();
        booth.festivalId = festivalId;
        booth.roadmapNodeId = roadmapNodeId;
        booth.boothName = boothName.trim();
        return booth;
    }

    public boolean belongsTo(Long festivalId) {
        return this.festivalId.equals(festivalId);
    }
}
