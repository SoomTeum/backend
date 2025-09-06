package com.comma.soomteum.domain.place.dto;

import lombok.*;

public final class TourApiRequestDto {

    private TourApiRequestDto() {}

    // =========================
    // 1) 위치기반 조회: /locationBasedList2
    // =========================
    @Getter @Setter
    @ToString @EqualsAndHashCode
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class LocationBasedList2 {
        private Double mapX;              // ex) 126.9784
        private Double mapY;              // ex) 37.5665
        private Integer radius;           // ex) 3000 (m)

        private String cat1;              // 대분류
        private String cat2;              // 중분류

        @Builder.Default private Integer pageNo = 1;
        @Builder.Default private Integer numOfRows = 20;
        @Builder.Default private String arrange = "E"; // 정렬코드 (A=제목순,C=수정일순, D=생성일순, E=거리순)
        @Builder.Default private String _type = "json";

        // 편의 기본값 (null 방어)
        public int pageNoOrDefault() { return pageNo == null ? 1 : pageNo; }
        public int rowsOrDefault()   { return numOfRows == null ? 10 : numOfRows; }
        public String arrangeOrDefault() { return (arrange == null || arrange.isBlank()) ? "E" : arrange; }

    }

    // =========================
    // 2) 지역기반 관광조회: /areaBasedList2
    // =========================
    @Getter @Setter
    @ToString @EqualsAndHashCode
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class AreaBasedList2 {
        private Integer areaCode;         // 시/도 코드
        private Integer sigunguCode;      // 시/군/구 코드
        private String cat1;
        private String cat2;
        private Integer contentTypeId;

        @Builder.Default private Integer pageNo = 1;
        @Builder.Default private Integer numOfRows = 20;
        @Builder.Default private String _type = "json";
        private String arrange;

        public int pageNoOrDefault() { return pageNo == null ? 1 : pageNo; }
        public int rowsOrDefault()   { return numOfRows == null ? 20 : numOfRows; }
        public String arrangeOrDefault() { return (arrange == null || arrange.isBlank()) ? "A" : arrange; }
    }


    // =========================
    // 3) 상세 관광지 조회: /DetailCommon2
    // =========================
    @Getter @Setter
    @ToString @EqualsAndHashCode
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DetailCommon2 {
        private String contentId;

        @Builder.Default private Integer pageNo = 1;
        @Builder.Default private Integer numOfRows = 1;

        public int pageNoOrDefault() { return pageNo == null ? 1 : pageNo; }
        public int rowsOrDefault()   { return numOfRows == null ? 1 : numOfRows; }
    }


    @Getter @Setter
    @ToString @EqualsAndHashCode
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class CategoryCode {
        private String cat1;
        private String cat2;
    }

}
