package com.comma.soomteum.domain.ai.dto;

import lombok.Value;

@Value
public class AiRecommendationResponse {
    String title;
    String contentid;
    String cat1;
    String cat2;
    String firstimage;
    String dist;
    String cnctrRate;
    //Integer rank; //1-5단계?
}