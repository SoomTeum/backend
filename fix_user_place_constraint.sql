-- 기존 제약조건 제거 및 올바른 제약조건 생성

USE soomteum;

-- 기존 유니크 제약조건 확인
SHOW INDEX FROM user_place WHERE Key_name LIKE '%unique%';

-- 기존 잘못된 유니크 제약조건 제거 (있는 경우)
-- ALTER TABLE user_place DROP INDEX user_place_unique;

-- 올바른 유니크 제약조건 생성 (user_id, place_id, type 조합)
ALTER TABLE user_place 
ADD CONSTRAINT uq_user_place_user_place_type 
UNIQUE (user_id, place_id, type);

-- 제약조건 확인
SHOW INDEX FROM user_place;