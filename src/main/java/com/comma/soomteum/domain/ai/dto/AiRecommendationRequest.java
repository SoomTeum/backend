package com.comma.soomteum.domain.ai.dto;

import java.util.List;

// 한 번의 추천 요청 전체를 담는 컨테이너
public record AiRecommendationRequest(
        List<Item> items
        // 필요하다면 가중치 같은 추가 정보도 여기에 포함 가능
        // double timeWeight,
        // double congestionWeight
) {
    // 요청서에 들어갈 개별 아이템의 형식
    public record Item(
            long placeId,
            String name,
            String theme,
            String region,
            int travelTimeInMinutes,
            double congestionRate
    ) {}
}