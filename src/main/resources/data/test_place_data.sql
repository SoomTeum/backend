-- 테스트용 Place 데이터 (강릉시 여행지)
-- Region과 Theme 데이터가 먼저 있어야 합니다.

-- 테스트용 Region 데이터 (강릉시)
INSERT IGNORE INTO region (region_id, name, kor_area_code, kor_sigungu_code, cnctr_area_code, created_at, updated_at) VALUES
(1, '강릉시', '32', '32230', '32230', NOW(), NOW());

-- 테스트용 Theme 데이터
INSERT IGNORE INTO theme (theme_id, cat1, cat2, name, created_at, updated_at) VALUES
(1, 'A01', 'A0101', '자연관광지', NOW(), NOW()),
(2, 'A02', 'A0201', '역사관광지', NOW(), NOW()),
(3, 'A03', 'A0302', '레포츠', NOW(), NOW());

-- 테스트용 Place 데이터 (강릉시 주요 관광지)
INSERT IGNORE INTO place (place_id, content_id, like_count, latitude, longitude, region_id, theme_id, created_at, updated_at) VALUES
(1, '264670', 0, 37.8056, 128.9084, 1, 1, NOW(), NOW()), -- 경포해수욕장 
(2, '264671', 0, 37.7881, 128.9342, 1, 1, NOW(), NOW()), -- 강문해수욕장
(3, '264672', 0, 37.7519, 128.8761, 1, 2, NOW(), NOW()), -- 강릉 중앙시장
(4, '264673', 0, 37.6924, 128.8172, 1, 3, NOW(), NOW()), -- 아르떼뮤지엄 강릉
(5, '264674', 0, 37.8969, 128.8269, 1, 1, NOW(), NOW()); -- 주문진해수욕장