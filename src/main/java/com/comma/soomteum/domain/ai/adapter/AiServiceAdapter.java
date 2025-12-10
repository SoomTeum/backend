package com.comma.soomteum.domain.ai.adapter;

import com.comma.soomteum.domain.ai.dto.AiRecommendationResponse;
import com.comma.soomteum.domain.ai.dto.AiRecommendationRequest;
import com.comma.soomteum.domain.ai.service.AiRecommendationService; // 새로운 서비스 사용
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AiServiceAdapter {

    private final AiRecommendationService recommendationService;

    // PlaceService가 호출할 메소드
    public List<AiRecommendationResponse> createRankedRecommendations(List<AiRecommendationRequest> items) {
        return recommendationService.createRankedRecommendations(items);
    }
}