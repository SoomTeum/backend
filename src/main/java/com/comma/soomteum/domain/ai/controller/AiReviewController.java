package com.comma.soomteum.domain.ai.controller;

import com.comma.soomteum.domain.ai.dto.AiReviewRequest;
import com.comma.soomteum.domain.ai.dto.AiReviewResponse;
import com.comma.soomteum.domain.ai.service.AiReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiReviewController {
    private final AiReviewService aiReviewService;

    @PostMapping("/summary")
    public ResponseEntity<AiReviewResponse> summarizeByPlaceName(@RequestBody AiReviewRequest request) throws IOException {
        AiReviewResponse response = aiReviewService.summarizeByPlaceName(request);
        return ResponseEntity.ok(response);
    }
}
