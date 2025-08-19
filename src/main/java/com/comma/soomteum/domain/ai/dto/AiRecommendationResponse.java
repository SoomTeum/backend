package com.comma.soomteum.domain.ai.dto;

import java.util.List;

// 최종 순위가 매겨진 추천 목록 전체를 담는 컨테이너
public record AiRecommendationResponse(
        List<RankedItem> items
) {
    public record RankedItem(
            long placeId,
            String name,
            String theme,
            String region,
            int rank // 최종 우선순위
    ) {}
}
