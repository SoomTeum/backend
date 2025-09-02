package com.comma.soomteum.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiReviewResponse {
    @JsonProperty("summary")
    private String summaryText;

}
