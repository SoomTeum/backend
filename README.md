# backend
“숨 여행, 틈” 프로젝트의 백엔드 리포지토리입니다.

---

## 🛠️ 개발 환경

- **Language**: Java 17
- **Framework**: Spring Boot 3.5.3
- **Build Tool**: Gradle
- **Database**: MySQL
- **ORM**: Spring Data JPA
- **Infra**: AWS EC2, RDS
- **API 문서화**: Swagger (springdoc-openapi)

---

## 📁 폴더 구조 (도메인형 DDD 기반)

```
backend
	domain/
	├── user/
	│   ├── controller/
	│   ├── service/
	│   ├── repository/
	│   ├── dto/
	│   └── entity/
	├── place/
	│   ├── controller/
	│   ├── service/
	│   ├── repository/
	│   ├── dto/
	│   └── entity/
	├── region/
	├── theme/
	├── userattraction/
	│   ├── controller/
	│   ├── service/
	│   ├── repository/
	│   ├── dto/
	│   └── entity/
	└── BaseEntity.java
	ㄴ global
		ㄴ exception

```


---

## 🔖 브랜치 전략 (GitHub Flow 기반)

- `main`: 제품 출시 브랜치 (배포 대상)
- `dev`: 통합 개발 브랜치 (optional)
- `feat/#issue-number`: 기능 개발용 브랜치
- `fix/#issue-number`: 버그 수정 브랜치

---

## 💬 PR 규칙

- 제목 형식: `feat: 로그인 API 구현`
- 템플릿 기반 작성 (유형 / 작업 내용 / 리뷰 포인트 등)

| 태그 | 설명 |
| --- | --- |
| ✨ [Feat] | 새로운 기능 추가 |
| 🐛 [Fix] | 버그 수정 |
| 🎨 [Design] | API 응답 포맷/UI 관련 수정 |
| 📝 [Docs] | 문서 수정 (README 등) |
| 🔧 [Chore] | 설정 변경, 의존성 관리 등 |
| 🚀 [Hotfix] | 배포 중 긴급 수정 |

---

## ✅ 커밋 컨벤션

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

## 🧾 Database 규칙

- **테이블 명**: `lower_snake_case`
- **PK 컬럼명**: `id`
- **기본 날짜 컬럼**: `created_at`, `updated_at`, `deleted_at`
- **작성자/수정자 필드**: `created_by`, `updated_by`
- **FK 명명 규칙**: `{참조테이블}_id`

---


