# 공영 주차장 기능 구현 문서

## 개요
강릉시 여행지 주변 공영 주차장 정보를 제공하는 기능을 구현했습니다. 실시간 주차 가능 대수 정보를 외부 API에서 가져와서 사용자에게 제공합니다.

## 주요 기능
1. **지역별 공영주차장 조회**: 특정 지역의 모든 공영주차장 목록 조회
2. **주변 공영주차장 조회**: 특정 좌표 기준으로 가까운 주차장들을 거리순으로 조회
3. **실시간 주차정보 업데이트**: 외부 API를 통한 실시간 주차 가능 대수 업데이트
4. **여행지-주차장 연동**: 여행지 상세 정보와 함께 주변 주차장 정보 제공

## 구현된 파일 구조

### 1. 엔티티 (Entity)
**`PublicParking.java`** - 공영주차장 정보를 저장하는 JPA 엔티티
- `prkId`: 주차장 고유 ID (unique)
- `prkName`: 주차장 이름
- `totalLots`: 전체 주차 구역 수
- `availLots`: 현재 이용 가능한 주차 구역 수
- `latitude`, `longitude`: 주차장 위치 좌표
- `regionCode`: 지역 코드 (강릉시: 32230)

### 2. 리포지토리 (Repository)
**`PublicParkingRepository.java`** - 주차장 데이터 액세스 계층
- `findByPrkId()`: 주차장 ID로 조회
- `findByRegionCode()`: 지역 코드별 주차장 조회
- `findNearbyParkingLots()`: 거리 기반 가까운 주차장 조회 (Haversine 공식 사용)

### 3. 서비스 (Service)
**`PublicParkingService.java`** - 주차장 관련 비즈니스 로직
- `findNearbyParking()`: 주변 주차장 조회 + 거리 계산
- `updateParkingAvailability()`: 실시간 주차 정보 업데이트
- `calculateDistance()`: Haversine 공식을 이용한 거리 계산
- `findByRegion()`: 지역별 주차장 목록 조회
- `findNearestParking()`: 가장 가까운 주차장 1개 조회

### 4. 컨트롤러 (Controller)
**`ParkingController.java`** - 주차장 관련 REST API 엔드포인트
- `GET /api/parking/region/{regionCode}`: 지역별 주차장 목록
- `GET /api/parking/nearby`: 주변 주차장 조회
- `POST /api/parking/update`: 실시간 주차 정보 업데이트

### 5. DTO (Data Transfer Object)
**`PublicParkingResponseDto.java`** - 클라이언트로 전송할 주차장 정보
- 거리 정보 포함/미포함 두 가지 변환 메서드 제공

## 여행지와 주차장 연동

### Place 서비스 연동
**`PlaceService.java`**에서 주차장 정보를 함께 제공:
- `getPlaceDetailWithParking()`: 여행지 상세 + 주변 주차장 5개
- `getPlaceDetailWithNearestParking()`: 여행지 상세 + 가장 가까운 주차장 1개
- 현재 강릉시(지역코드: 32) 여행지만 주차장 정보 제공

### 응답 DTO
**`PlaceDetailWithParkingDto.java`** - 여행지 정보 + 주차장 목록을 함께 반환

## 테스트 데이터 초기화

**`DataInitializer.java`**에서 개발/테스트용 주차장 데이터 생성:
- 강릉시 주요 주차장 5개소 초기 데이터
- 성내동광장주차장, 강문제2공영주차장, 주문진해안주차타워 등

## API 설정

### 설정 파일 (`application.yml`)
```yaml
parking:
  api:
    key: ${PARKING_API_KEY:}
    url: https://www.parking.go.kr/api/getParkRltm
```

### WebClient 설정
**`ParkingConfig.java`** - 외부 주차장 API 호출을 위한 WebClient 빈 설정

## 주요 기술적 특징

1. **거리 계산**: Haversine 공식을 사용한 정확한 거리 계산
2. **실시간 업데이트**: 외부 API 연동으로 실시간 주차 가능 대수 갱신
3. **지역 필터링**: 강릉시(32230) 지역만 주차장 정보 제공
4. **장애 대응**: 외부 API 호출 실패 시 캐시된 데이터 사용
5. **트랜잭션 관리**: 읽기 전용 트랜잭션과 업데이트 트랜잭션 분리

## API 사용 예시

### 1. 주변 주차장 조회
```http
GET /api/parking/nearby?latitude=37.8056&longitude=128.9084&regionCode=32230&limit=5
```

### 2. 지역별 주차장 목록
```http
GET /api/parking/region/32230
```

### 3. 실시간 정보 업데이트
```http
POST /api/parking/update
```

## 데이터베이스 스키마

```sql
CREATE TABLE public_parking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prk_id VARCHAR(255) NOT NULL UNIQUE,
    prk_name VARCHAR(255) NOT NULL,
    total_lots INTEGER,
    avail_lots INTEGER,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    region_code VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

## 향후 확장 가능성

1. **지역 확대**: 강릉시 외 다른 지역 주차장 정보 추가
2. **실시간 알림**: 주차 가능 대수 변화 시 알림 기능
3. **예약 기능**: 주차장 사전 예약 시스템 연동
4. **결제 연동**: 주차 요금 결제 시스템 통합
5. **주차장 리뷰**: 사용자 리뷰 및 평점 시스템