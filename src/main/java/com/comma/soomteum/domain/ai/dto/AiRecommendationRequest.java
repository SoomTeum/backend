package com.comma.soomteum.domain.ai.dto;

import lombok.Value;

import java.util.List;

@Value
public class AiRecommendationRequest {
    String title;
    String contentid;
    String cat1;
    String cat2;
    String firstimage;
    String dist;
    String cnctrRate;
}