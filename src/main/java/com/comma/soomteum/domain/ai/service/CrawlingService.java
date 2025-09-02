package com.comma.soomteum.domain.ai.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class CrawlingService {
    /**
     * API테스트용 더미 데이터
     * 실제 크롤링 대신, 테스트용 리뷰 데이터를 반환.
     */
    public List<String> fetchReviewsByPlaceName(String placeName) {
        System.out.println("!!!!!!!!!! [개발 모드] !!!!!!!!!!!");
        System.out.println("'" + placeName + "'에 대한 실제 크롤링을 생략하고 더미 데이터를 반환.");

        List<String> dummyReviews = new ArrayList<>();

        dummyReviews.add(placeName + "은(는) 주차가 편리하고 주변 경치가 아름답다는 리뷰가 많았습니다.");
        dummyReviews.add("가족 단위 방문객들에게 특히 인기가 많으며, 시설이 깨끗하다는 장점이 있습니다. 다만, 주말에는 인파가 몰려 혼잡할 수 있다는 점은 참고해야 합니다.");
        dummyReviews.add("대중교통 접근성은 다소 아쉽다는 의견이 있었지만, 전반적으로 만족도가 높은 여행지입니다.");

        return dummyReviews;
    }
}