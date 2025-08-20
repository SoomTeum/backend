package com.comma.soomteum.domain.ai.service;

import com.comma.soomteum.domain.ai.dto.AiRecommendationRequest;
import com.comma.soomteum.domain.ai.dto.AiRecommendationResponse;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AiRecommendationScoreDebugTest {

    private final AiRecommendationService service = new AiRecommendationService();

    // ==== 서비스와 동일 상수들 (테스트 내 재현) ====
    private static final double EPS = 1e-9;
    private static final double DEFAULT_SHARPNESS = 0.1;
    private static final double FIXED_PENALTY_SCORE = 20.0;
    private static final double SHRINKAGE_LAMBDA = 0.7;
    private static final double MAX_SHARPNESS = 10.0;

    // ===== 여기 데이터만 바꿔가며 테스트하세요 =====
    @Test
    void printScores_withYourData() {
        List<AiRecommendationRequest.Item> items = List.of(
                row(101L, "Alpha",     30, 0.10),
                row(102L, "Bravo",     60, 0.20),
                row(103L, "Charlie",   90, 0.35),
                row(104L, "Delta",     45, -1.0),
                row(105L, "Echo",     120, 0.50),
                row(106L, "Foxtrot",   15, 0.05),
                row(107L, "Golf",     180, 0.80),
                row(108L, "Hotel",     75, 0.25),
                row(109L, "India",     55, -1.0),
                row(110L, "Juliet",    65, 0.20),
                row(111L, "Kilo",      65, 0.20),
                row(112L, "Lima",      45, 0.10),
                row(113L, "Mike",      45, 0.10),
                row(114L, "November", 100, -1.0),
                row(115L, "Oscar",     25, 0.40),
                row(116L, "Papa",     140, 0.15),
                row(117L, "Quebec",    35, 0.30),
                row(118L, "Romeo",    200, 1.20),
                row(119L, "Sierra",    85, 0.05),
                row(120L, "Tango",     60, 0.50)
        );

        // 1) 서비스 호출 → 정답 랭킹
        AiRecommendationResponse res = service.createRankedRecommendations(new AiRecommendationRequest(items));

        // 2) 동일 공식으로 점수 재현 (프린트용)
        var rows = computeScores(items);

        // 3) 서비스와 같은 정렬: total DESC → time ASC → name ASC
        rows.sort(
                Comparator.comparingDouble(Row::totalScore).reversed()
                        .thenComparingInt(Row::time)
                        .thenComparing(Row::name, Comparator.nullsLast(String::compareTo))
        );
        for (int i = 0; i < rows.size(); i++) rows.get(i).rank = i + 1;

        // 4) 서비스 응답의 placeId 순서와 일치 확인 (간단 검증)
        var serviceOrder = res.items().stream().map(AiRecommendationResponse.RankedItem::placeId).toList();
        var debugOrder   = rows.stream().map(Row::placeId).toList();
        assertEquals(serviceOrder, debugOrder, "서비스 랭킹과 디버그 랭킹이 다릅니다.");

        // 5) 보기 좋은 표로 출력
        printTable(rows);

        // 6) 전역 파라미터도 함께 출력
        var g = globals(items);
        System.out.printf("%n[Globals] timeSharpness=%.6f, congestionSharpness=%.6f, medianTime=%.3f, medianRate=%.3f, finalPenalty=%.3f%n",
                g.timeSharpness, g.congestionSharpness, g.medianTime, g.medianRate, g.finalPenalty);
    }

    // ====== 헬퍼/유틸 ======
    private static AiRecommendationRequest.Item row(long id, String name, int travelMin, double congestion) {
        return new AiRecommendationRequest.Item(id, name, "theme", "region", travelMin, congestion);
    }

    private static double sigmoid(double x) { return 1.0 / (1.0 + exp(-x)); }

    private static double median(double[] sorted) {
        if (sorted.length == 0) return 0;
        int m = sorted.length / 2;
        return (sorted.length % 2 == 1) ? sorted[m] : (sorted[m - 1] + sorted[m]) / 2.0;
    }

    private static double quantile(double[] sorted, double p) {
        if (sorted.length == 0) return 0;
        if (sorted.length == 1) return sorted[0];
        double pos = p * (sorted.length - 1);
        int lo = (int) floor(pos), hi = (int) ceil(pos);
        double w = pos - lo;
        return sorted[lo] * (1 - w) + sorted[hi] * w;
    }

    // 서비스의 List용 분위수(인덱스 픽) 로직 재현
    private static double quantileListIndexPick(List<Double> sortedScores, double p) {
        if (sortedScores.isEmpty()) return 0;
        int idx = (int) ceil(p * sortedScores.size()) - 1;
        return sortedScores.get(Math.max(0, idx));
    }

    private static class Globals {
        double timeSharpness, congestionSharpness, medianTime, medianRate, finalPenalty;
    }

    private static Globals globals(List<AiRecommendationRequest.Item> items) {
        double[] timesSorted = items.stream().mapToDouble(AiRecommendationRequest.Item::travelTimeInMinutes).sorted().toArray();
        double[] ratesValidSorted = items.stream().mapToDouble(AiRecommendationRequest.Item::congestionRate).filter(r -> r >= 0).sorted().toArray();

        double timeIqr = (timesSorted.length < 2) ? 0 : quantile(timesSorted, 0.75) - quantile(timesSorted, 0.25);
        double rateIqr = (ratesValidSorted.length < 2) ? 0 : quantile(ratesValidSorted, 0.75) - quantile(ratesValidSorted, 0.25);

        double timeSharpness = (timeIqr <= EPS) ? DEFAULT_SHARPNESS : min(log(9) / timeIqr, MAX_SHARPNESS);
        double congestionSharpness = (rateIqr <= EPS) ? DEFAULT_SHARPNESS : min(log(9) / rateIqr, MAX_SHARPNESS);

        double medianTime = median(timesSorted);
        double medianRate = (ratesValidSorted.length > 0) ? median(ratesValidSorted) : 0.0;

        List<Double> validCongScores = new ArrayList<>();
        if (ratesValidSorted.length > 0) {
            for (double r : ratesValidSorted) {
                validCongScores.add(sigmoid((medianRate - r) * congestionSharpness) * 100.0);
            }
        }
        validCongScores.sort(Double::compareTo);
        double dynamicPenalty = validCongScores.isEmpty() ? FIXED_PENALTY_SCORE : quantileListIndexPick(validCongScores, 0.2);
        double finalPenaltyScore = (SHRINKAGE_LAMBDA * FIXED_PENALTY_SCORE) + ((1 - SHRINKAGE_LAMBDA) * dynamicPenalty);

        Globals g = new Globals();
        g.timeSharpness = timeSharpness;
        g.congestionSharpness = congestionSharpness;
        g.medianTime = medianTime;
        g.medianRate = medianRate;
        g.finalPenalty = finalPenaltyScore;
        return g;
    }

    private static class Row {
        long placeId; String name; int time; double congestion;
        double timeScore, congestionScore; Double penalty; double totalScore;
        int rank;

        long placeId() { return placeId; }
        String name()  { return name; }
        int time()     { return time; }
        double totalScore() { return totalScore; }
    }

    private static List<Row> computeScores(List<AiRecommendationRequest.Item> items) {
        Globals g = globals(items);
        List<Row> rows = new ArrayList<>();
        for (var it : items) {
            Row r = new Row();
            r.placeId = it.placeId();
            r.name = it.name();
            r.time = it.travelTimeInMinutes();
            r.congestion = it.congestionRate();

            r.timeScore = sigmoid((g.medianTime - it.travelTimeInMinutes()) * g.timeSharpness) * 100.0;
            if (it.congestionRate() < 0) {
                r.congestionScore = g.finalPenalty;
                r.penalty = g.finalPenalty;
            } else {
                r.congestionScore = sigmoid((g.medianRate - it.congestionRate()) * g.congestionSharpness) * 100.0;
            }
            r.totalScore = (r.timeScore * 0.55) + (r.congestionScore * 0.45);
            rows.add(r);
        }
        return rows;
    }

    private static void printTable(List<Row> ranked) {
        String header = String.format("%-5s %-8s %-12s %-12s %-12s %-16s %-10s %-12s",
                "Rank", "PlaceID", "Time(min)", "Congestion", "TimeScore", "CongestionScore", "Penalty", "TotalScore");
        System.out.println("\n" + header);
        System.out.println("=".repeat(header.length()));
        for (Row r : ranked) {
            System.out.printf("%-5d %-8d %-12d %-12.3f %-12.3f %-16.3f %-10s %-12.3f%n",
                    r.rank, r.placeId, r.time, r.congestion, r.timeScore, r.congestionScore,
                    (r.penalty == null ? "-" : String.format("%.3f", r.penalty)), r.totalScore);
        }
        System.out.println();
    }
}