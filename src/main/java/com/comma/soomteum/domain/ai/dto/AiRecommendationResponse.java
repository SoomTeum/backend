package com.comma.soomteum.domain.ai.dto;

import java.util.List;

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
