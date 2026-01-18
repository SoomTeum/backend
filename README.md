# SoomTeum (숨틈)
### 2025 관광데이터 활용 공모전 장려상 수상작 🎖️
> **숨 여행, 틈** - 한적한 여행지를 발견하고, 나만의 여행을 계획하세요.

SoomTeum은 여행지 추천 및 관리 플랫폼의 Spring Boot 기반 REST API 백엔드입니다.

관광 데이터 API(https://www.data.go.kr/index.do)를 활용하여 혼잡도 정보/실시간 주차 정보를 제공합니다.

---

## 기술 스택

| 분류 | 기술                                                     |
|------|--------------------------------------------------------|
| **Framework** | Spring Boot 3.5.3, Spring WebFlux, Spring Security 6.x |
| **Language** | Java 17                                                |
| **Database** | MySQL 8.0, Spring Data JPA                             |
| **Authentication** | JWT (JJWT 0.11.5), Kakao OAuth 2.0                     |
| **Cache** | Caffeine 3.1.8                                         |
| **Documentation** | Springdoc OpenAPI (Swagger UI)                         |
| **External API** | 공공데이터포털 (한국관광공사 API), Google Gemini AI    |
| **Infrastructure** | Docker, AWS (EC2, RDS)                                 |
| **Testing** | JUnit 5, k6 (부하 테스트)                                   |

---

## 주요 기능

### 인증 및 사용자 관리
- Kakao OAuth 2.0 소셜 로그인
- JWT 기반 인증 (Access Token 14일, Refresh Token 7일)
- 프로필 조회/수정, 회원 탈퇴

### 여행지 탐색
- **위치 기반 추천**: 좌표 및 반경으로 주변 여행지 검색
- **지역/테마 기반 필터링**: 지역과 테마별 여행지 조회
- **혼잡도 정보**: 실시간 혼잡도 수준 제공
- **AI 리뷰 요약**: Google Gemini를 활용한 리뷰 요약

### 사용자 상호작용
- 여행지 좋아요 (Like)
- 여행지 저장 (Bookmark)
- 저장한 여행지 검색 및 페이지네이션

### 주차 정보
- 지역별 공영주차장 조회
- 좌표 기반 근처 주차장 검색 (Haversine 거리 계산)
- 실시간 주차 가용 현황 업데이트

---

## 프로젝트 구조

```
src/main/java/com/comma/soomteum/
├── config/                     # 전역 설정 (Security, Swagger, Cache 등)
├── domain/                     # 도메인 모듈 (DDD 기반)
│   ├── ai/                    # AI 리뷰 요약 서비스
│   ├── auth/                  # Kakao OAuth, JWT 인증
│   ├── external/tourapi/      # 공공 관광 API 연동
│   ├── parking/               # 공영주차장 정보
│   ├── place/                 # 여행지 관리
│   ├── region/                # 지역 정보
│   ├── theme/                 # 여행 테마 분류
│   ├── token/                 # 토큰 관리
│   ├── user/                  # 사용자 관리
│   ├── userPlace/             # 좋아요/저장 기능
│   └── recommendation/        # 여행지 추천 서비스
└── global/                     # 전역 유틸리티
    ├── exception/             # 예외 처리
    └── response/              # API 응답 래퍼
```

---

## API 문서

애플리케이션 실행 후 Swagger UI에서 API 문서를 확인할 수 있습니다:

```
http://localhost:8080/swagger-ui.html
```

### 주요 API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/auth/login/kakao` | Kakao 로그인 |
| `POST` | `/api/auth/reissue` | 토큰 재발급 |
| `GET` | `/api/my/profile` | 프로필 조회 |
| `GET` | `/api/places/integrated/{contentId}` | 여행지 통합 상세 정보 |
| `GET` | `/api/places` | 지역/테마 기반 여행지 목록 |
| `GET` | `/api/places/ai` | 위치 기반 여행지 추천 |
| `POST` | `/api/places/like` | 여행지 좋아요 |
| `PUT` | `/api/my/places/save` | 여행지 저장 |
| `GET` | `/api/my/places` | 저장한 여행지 목록 |
| `GET` | `/api/parking/nearby` | 근처 주차장 조회 |
| `POST` | `/api/ai/summary` | AI 리뷰 요약 |

---

## 성능 최적화

### Caffeine 캐시 아키텍처

5단계 캐시 전략을 적용하여 외부 API 호출을 최소화했습니다:

| 캐시 | TTL | 최대 항목 | 대상 |
|------|-----|-----------|------|
| tourApiCache | 1시간 | 500 | 외부 관광 API |
| cnctrRateCache | 30분 | 1,000 | 혼잡도 데이터 |
| themeCache | 24시간 | 100 | 테마 메타데이터 |
| regionCache | 24시간 | 300 | 지역 메타데이터 |
| placeLikeCache | 10분 | 500 | 좋아요 수 |

**성능 개선**: p95 응답 시간 2,400ms → 15.29ms (99.4% 개선)

---

## 브랜치 전략 (GitHub Flow 기반)

- `main`: 제품 출시 브랜치 (배포 대상)
- `dev`: 통합 개발 브랜치 (optional)
- `feat/#issue-number`: 기능 개발용 브랜치
- `fix/#issue-number`: 버그 수정 브랜치

---

## PR 규칙

- 제목 형식: `feat: 로그인 API 구현`
- 템플릿 기반 작성 (유형 / 작업 내용 / 리뷰 포인트 등)

| 태그 | 설명 |
| --- | --- |
| [Feat] | 새로운 기능 추가 |
| [Fix] | 버그 수정 |
| [Design] | API 응답 포맷/UI 관련 수정 |
| [Docs] | 문서 수정 (README 등) |
| [Chore] | 설정 변경, 의존성 관리 등 |
| [Hotfix] | 배포 중 긴급 수정 |

---

## 커밋 컨벤션

```bash
<태그>: <제목>

- 작업 내용 상세
- 작업 내용 상세2

#이슈번호 (optional)
```

### 예시

- `feat: 회원가입 API 구현`
- `fix: 회원가입 시 닉네임 중복 오류 수정`

---

## Database 규칙

- **테이블 명**: `lower_snake_case`
- **PK 컬럼명**: `id`
- **기본 날짜 컬럼**: `created_at`, `updated_at`, `deleted_at`
- **작성자/수정자 필드**: `created_by`, `updated_by`
- **FK 명명 규칙**: `{참조테이블}_id`

---

## 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.

---

## 시작하기

### 사전 요구사항

- Java 17
- MySQL 8.0+
- Gradle 8.4+

### 환경 변수 설정

`application.properties` 또는 환경 변수로 설정:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/soomteum?serverTimezone=Asia/Seoul
spring.datasource.username=your_username
spring.datasource.password=your_password

# JWT
jwt.secret=your_base64_encoded_secret

# Kakao OAuth
spring.security.oauth2.client.registration.kakao.client-id=your_kakao_client_id
spring.security.oauth2.client.registration.kakao.redirect-uri=http://localhost:8080/api/auth/kakao/callback

# 공공데이터 API
tourapi.clients.korService2.service-key=your_tour_api_key
parking.api.key=your_parking_api_key

# Google Gemini AI
gemini.api.key=your_gemini_api_key
```

### 데이터베이스 생성

```sql
CREATE DATABASE soomteum CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 빌드 및 실행

```bash
# 프로젝트 빌드
./gradlew clean build

# 애플리케이션 실행
./gradlew bootRun

# 또는 JAR 직접 실행
java -jar build/libs/soomteum-0.0.1-SNAPSHOT.jar
```

### Docker 실행

```bash
# 이미지 빌드
docker build -t soomteum:latest .

# 컨테이너 실행
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/soomteum \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  soomteum:latest
```
