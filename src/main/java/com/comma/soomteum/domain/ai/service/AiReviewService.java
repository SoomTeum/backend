package com.comma.soomteum.domain.ai.service;

import com.comma.soomteum.domain.ai.dto.AiReviewRequest;
import com.comma.soomteum.domain.ai.dto.AiReviewResponse;
import com.comma.soomteum.domain.ai.infra.client.GeminiApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiReviewService {

    private final GeminiApiClient geminiApiClient;
    private final CrawlingService crawlingService;

    @Value("${GEMINI_API_KEY}")
    private String geminiKey;

    public AiReviewResponse summarizeByPlaceName(AiReviewRequest req) throws IOException {
        // 1. 크롤링 서비스 호출
        List<String> reviewTexts = crawlingService.fetchReviewsByPlaceName(req.getPlaceName());

        if (reviewTexts.isEmpty()) {
            return new AiReviewResponse("요약할 리뷰를 찾을 수 없습니다.");
        }

        // 2. LLM에게 보낼 프롬프트 생성
        String prompt = buildPrompt(reviewTexts);

        // 3. Gemini API 호출
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );
        Map<String, Object> rawResponse = geminiApiClient.generateContent(geminiKey, requestBody);

        // 4. API 응답에서 요약 텍스트 추출 및 DTO로 변환
        return extractSummary(rawResponse);
    }

    private String buildPrompt(List<String> reviewTexts) {
        String combinedReviews = reviewTexts.stream()
                .collect(Collectors.joining("\n"));

        return String.format(
                "아래 리뷰들을 종합해서 방문객에게 유용한 정보만 담아 자연스러운 한 문단으로 요약해줘. 긍정적인 점과 아쉬운 점을 모두 포함해줘. 다른 설명 없이 요약된 문장만 말해줘.\n\n--- 리뷰 본문 ---\n%s",
                combinedReviews
        );
    }

    private AiReviewResponse extractSummary(Map<String, Object> rawResponse) {
        try {
            List<Object> candidates = (List<Object>) rawResponse.get("candidates");
            Map<String, Object> firstCandidate = (Map<String, Object>) candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            List<Object> parts = (List<Object>) content.get("parts");
            Map<String, Object> firstPart = (Map<String, Object>) parts.get(0);
            String summaryText = (String) firstPart.get("text");

            return new AiReviewResponse(summaryText.trim());
        } catch (Exception e) {
            // !!로그추가 예정
            return new AiReviewResponse("AI 응답을 처리하는 중 오류가 발생했습니다.");
        }
    }
}
