package com.comma.soomteum.domain.ai.service;

import com.comma.soomteum.domain.ai.dto.AiRecommendationRequest;
import com.comma.soomteum.domain.ai.dto.AiRecommendationResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class AiRecommendationService {

    private record ScoredItem(AiRecommendationRequest originalItem, double totalScore) {}

    // 샤프니스 튜닝 기본값 기준
    private static final double EPS = 1e-9;
    // --- 튜닝 가능한 상수들 ---
    private static final double DEFAULT_SHARPNESS = 0.1;
    private static final double FIXED_PENALTY_SCORE = 20.0;
    private static final double SHRINKAGE_LAMBDA = 0.7;
    private static final double MAX_SHARPNESS = 10.0;
    private static final double MIN_SHARPNESS = 0.01;
    // 절대적 혼잡도 레벨 기준
    private static final double CONGESTION_LEVEL_1_THRESHOLD = 30.0; // 쾌적
    private static final double CONGESTION_LEVEL_2_THRESHOLD = 70.0; // 보통
    private static final double CONGESTION_LEVEL_3_THRESHOLD = 90.0; // 붐빔


    public List<AiRecommendationResponse> createRankedRecommendations(List<AiRecommendationRequest> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }


        // --- 데이터 준비 및 동적 샤프니스 계산 ---
        double[] sortedDists = items.stream().mapToDouble(item -> parseDouble(item.getDist())).sorted().toArray();
        double[] validRates = items.stream()
                .mapToDouble(item -> parseDouble(item.getCnctrRate()))
                .filter(r -> r >= 0)
                .sorted().toArray();

        double distIqr = findIqr(sortedDists); // <<< 가독성을 위해 변수명 변경 (timeIqr -> distIqr)
        double rateIqr = findIqr(validRates);

        double distSharpness = (distIqr <= EPS) ? DEFAULT_SHARPNESS : Math.min(Math.log(9) / distIqr, MAX_SHARPNESS);
        double congestionSharpness = (rateIqr <= EPS) ? DEFAULT_SHARPNESS : Math.min(Math.log(9) / rateIqr, MAX_SHARPNESS);

        double medianDist = findMedian(sortedDists); // <<< 가독성을 위해 변수명 변경 (medianTime -> medianDist)
        double medianRate = (validRates.length > 0) ? findMedian(validRates) : 0.0;


        // --- 동적 패널티 계산 ---
        List<Double> validCongestionScores = new ArrayList<>();
        if (validRates.length > 0) {
            for (double rate : validRates) {
                validCongestionScores.add(sigmoid((medianRate - rate) * congestionSharpness) * 100.0);
            }
        }
        validCongestionScores.sort(Double::compare);
        double dynamicPenalty = validCongestionScores.isEmpty() ? FIXED_PENALTY_SCORE : findQuantile(validCongestionScores, 0.2);
        double finalPenaltyScore = (SHRINKAGE_LAMBDA * FIXED_PENALTY_SCORE) + ((1 - SHRINKAGE_LAMBDA) * dynamicPenalty);

        // --- 최종 점수 계산 ---
        List<ScoredItem> scoredItems = new ArrayList<>();
        for (AiRecommendationRequest item : items) {
            double distValue = parseDouble(item.getDist());
            double rateValue = parseDouble(item.getCnctrRate());

            double distScore = sigmoid((medianDist - distValue) * distSharpness) * 100.0;
            double congestionScore;
            if (rateValue < 0) {
                congestionScore = finalPenaltyScore;
            } else {
                congestionScore = sigmoid((medianRate - rateValue) * congestionSharpness) * 100.0;
            }

            double totalScore = (distScore * 0.55) + (congestionScore * 0.45);
            scoredItems.add(new ScoredItem(item, totalScore));
        }


        // --- 정렬 및 최종 응답 생성 ---
        scoredItems.sort(
                Comparator.comparingDouble(ScoredItem::totalScore).reversed()
                        .thenComparing(si -> parseDouble(si.originalItem().getDist()))
                        .thenComparing(si -> si.originalItem().getTitle())
        );

        List<AiRecommendationResponse> rankedItems = scoredItems.stream()
                .map(scoredItem -> {
                    AiRecommendationRequest original = scoredItem.originalItem();

                    // 추가: 각 아이템의 혼잡도 랭킹 계산
                    double rateValue = parseDouble(original.getCnctrRate());
                    int level = determineCongestionLevel(rateValue);

                    AiRecommendationResponse dto = new AiRecommendationResponse();

                    dto.setTitle(original.getTitle());
                    dto.setContentid(original.getContentid());
                    dto.setCat1(original.getCat1());
                    dto.setCat2(original.getCat2());
                    dto.setFirstimage(original.getFirstimage());
                    dto.setDist(original.getDist());
                    dto.setCnctrRate(original.getCnctrRate());
                    dto.setCongestionLevel(level);
                    System.out.println(dto.getCongestionLevel());
                    return dto;
                })
                .collect(Collectors.toList());

        return rankedItems;
    }


    // --- 헬퍼 메소드들 ---

    // 추가: 혼잡도 랭킹을 결정하는 헬퍼 메소드
    private int determineCongestionLevel(double rateValue) {
        if (rateValue < 0) { // 데이터가 없는 경우 (-1, -2 등)
            return -1; // 레벨 0: 정보 없음
        }
        if (rateValue <= CONGESTION_LEVEL_1_THRESHOLD) {
            return 1; // 레벨 1: 쾌적
        } else if (rateValue <= CONGESTION_LEVEL_2_THRESHOLD) {
            return 2; // 레벨 2: 보통
        } else if (rateValue <= CONGESTION_LEVEL_3_THRESHOLD) {
            return 3; // 레벨 3: 붐빔
        } else {
            return 4; // 레벨 4: 매우 붐빔
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException e) {
            // 파싱 실패 시 -1.0 (결측치)과 동일하게 처리
            return -1.0;
        }
    }

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