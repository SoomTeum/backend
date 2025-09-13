package com.comma.soomteum.domain.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;

@Getter
@Setter
@NoArgsConstructor
public class AiRecommendationResponse {
    String title;
    String contentid;
    String cat1;
    String cat2;
    String firstimage;
    String dist;
    String cnctrRate;
    int congestionLevel; //여행지 rank 추가
}