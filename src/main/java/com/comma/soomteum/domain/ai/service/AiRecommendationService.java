package com.comma.soomteum.domain.ai.service;

import com.comma.soomteum.domain.ai.dto.AiRecommendationRequest;
import com.comma.soomteum.domain.ai.dto.AiRecommendationResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class AiRecommendationService {

    // 내부 계산용 임시 데이터 클래스
    private record ScoredItem(AiRecommendationRequest.Item originalItem, double totalScore) {}

    // 샤프니스 튜닝 기본값 기준
    private static final double EPS = 1e-9;
    // --- 튜닝 가능한 상수들 ---
    private static final double DEFAULT_SHARPNESS = 0.1; // IQR이 0일 때 사용할 기본 샤프니스
    private static final double FIXED_PENALTY_SCORE = 20.0; // 고정 패널티 값
    private static final double SHRINKAGE_LAMBDA = 0.7; // 수축 가중치 (고정값에 70% 비중)
    private static final double MAX_SHARPNESS = 10.0;        // 샤프니스 상한선
    private static final double MIN_SHARPNESS = 0.01;


    public AiRecommendationResponse createRankedRecommendations(AiRecommendationRequest request) {
        List<AiRecommendationRequest.Item> items = request.items();
        if (items == null || items.isEmpty()) {
            return new AiRecommendationResponse(List.of());
        }

        // --- 데이터 준비 및 동적 샤프니스 계산 ---
        double[] sortedTimes = items.stream().mapToDouble(AiRecommendationRequest.Item::travelTimeInMinutes).sorted().toArray();
        double[] validRates = items.stream().mapToDouble(AiRecommendationRequest.Item::congestionRate).filter(r -> r >= 0).sorted().toArray();

        double timeIqr = findIqr(sortedTimes);
        double rateIqr = findIqr(validRates);

        // IQR을 기반으로 샤프니스 자동 튜닝 (IQR이 0이면 기본값 사용)
        double timeSharpness = (timeIqr <= EPS) ? DEFAULT_SHARPNESS : Math.min(Math.log(9) / timeIqr, MAX_SHARPNESS);
        double congestionSharpness = (rateIqr <= EPS) ? DEFAULT_SHARPNESS : Math.min(Math.log(9) / rateIqr, MAX_SHARPNESS);

        double medianTime = findMedian(sortedTimes);
        double medianRate = (validRates.length > 0) ? findMedian(validRates) : 0.0;


        // --- 동적 패널티 계산 ---
        List<Double> validCongestionScores = new ArrayList<>();
        if (validRates.length > 0) {
            for (double rate : validRates) {
                validCongestionScores.add(sigmoid((medianRate - rate) * congestionSharpness) * 100.0);
            }
        }
        validCongestionScores.sort(Double::compare); // 분위수 계산 전 반드시 정렬!
        double dynamicPenalty = validCongestionScores.isEmpty() ? FIXED_PENALTY_SCORE : findQuantile(validCongestionScores, 0.2); // p20 (하위 20%)
        double finalPenaltyScore = (SHRINKAGE_LAMBDA * FIXED_PENALTY_SCORE) + ((1 - SHRINKAGE_LAMBDA) * dynamicPenalty);


        // --- 최종 점수 계산 ---
        List<ScoredItem> scoredItems = new ArrayList<>();
        for (AiRecommendationRequest.Item item : items) {
            double timeScore = sigmoid((medianTime - item.travelTimeInMinutes()) * timeSharpness) * 100.0;
            double congestionScore;
            if (item.congestionRate() < 0) {
                congestionScore = finalPenaltyScore;
            } else {
                congestionScore = sigmoid((medianRate - item.congestionRate()) * congestionSharpness) * 100.0;
            }

            double totalScore = (timeScore * 0.55) + (congestionScore * 0.45);
            scoredItems.add(new ScoredItem(item, totalScore));
        }


        // --- 정렬 및 최종 응답 생성 ---
        scoredItems.sort(
                Comparator.comparingDouble(ScoredItem::totalScore).reversed()
                        .thenComparing(si -> si.originalItem().travelTimeInMinutes()) // 1차 동점: 시간 짧은 순
                        .thenComparing(si -> si.originalItem().name())              // 2차 동점: 이름 가나다 순
        );

        List<AiRecommendationResponse.RankedItem> rankedItems = IntStream.range(0, scoredItems.size())
                .mapToObj(i -> {
                    ScoredItem scoredItem = scoredItems.get(i);
                    AiRecommendationRequest.Item original = scoredItem.originalItem();
                    return new AiRecommendationResponse.RankedItem(
                            original.placeId(),
                            original.name(),
                            original.theme(),
                            original.region(),
                            i + 1 // 랭킹 부여
                    );
                })
                .collect(Collectors.toList());

        return new AiRecommendationResponse(rankedItems);
    }


    // --- 헬퍼 메소드들 ---
    private double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    private double findMedian(double[] sortedData) {
        if (sortedData.length == 0) return 0;
        int middle = sortedData.length / 2;
        return (sortedData.length % 2 == 1) ? sortedData[middle] : (sortedData[middle - 1] + sortedData[middle]) / 2.0;
    }

    // 배열을 위한 분위수 계산 메소드
    private double findQuantile(double[] sortedData, double percentile) {
        if (sortedData.length == 0) return 0;
        if (sortedData.length == 1) return sortedData[0];

        double pos = percentile * (sortedData.length - 1);
        int lower = (int) Math.floor(pos);
        int upper = (int) Math.ceil(pos);
        double weight = pos - lower;

        if (upper >= sortedData.length) return sortedData[sortedData.length - 1];
        return sortedData[lower] * (1 - weight) + sortedData[upper] * weight;
    }

    // 리스트를 위한 분위수 계산 메소드 (오버로딩)
    private double findQuantile(List<Double> sortedScores, double percentile) {
        if (sortedScores.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile * sortedScores.size()) - 1;
        return sortedScores.get(Math.max(0, index));
    }

    // IQR(사분위수 범위) 계산 메소드
    private double findIqr(double[] sortedData) {
        if (sortedData.length < 2) return 0;
        double q1 = findQuantile(sortedData, 0.25);
        double q3 = findQuantile(sortedData, 0.75);
        return q3 - q1;
    }
}