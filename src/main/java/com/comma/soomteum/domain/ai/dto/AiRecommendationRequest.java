package com.comma.soomteum.domain.ai.dto;

import java.util.List;

public record AiRecommendationRequest(
        List<Item> items
        // 필요하다면 가중치 같은 추가 정보도 여기에 포함 가능
        // double timeWeight,
        // double congestionWeight
) {
    public record Item(
            long placeId,
            String name,
            String theme,
            String region,
            int travelTimeInMinutes,
            double congestionRate
    ) {}
}