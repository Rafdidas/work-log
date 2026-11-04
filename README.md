# Work Log

매일 1분 안에 "오늘 무엇에 얼마나 썼는지"를 기록하고, 그 기록으로 **주간 보고 초안**과 **기간별 투입 공수(MD)** 를 바로 뽑는 개인용 업무 기록 도구입니다. 풀스택 포트폴리오를 겸해, 지금은 한 명이 쓰지만 팀 단위로 넓힐 수 있는 구조(사용자 단위 데이터 격리, 팀 테이블)로 만들었습니다.

## 왜 만들었나

- 매주 주간 보고를 쓰는데, 지난 일을 기억과 흩어진 메모로 다시 맞추느라 시간이 듭니다.
- 가끔 "4월부터 ○○에 공수가 얼마나 들어갔는지" 같은 원가분석 요청이 옵니다. 그때 바로 답하려면 매일의 기록이 **월별로 합산 가능한 형태**로 남아 있어야 합니다.

## 주요 기능

| 화면 | 하는 일 |
|---|---|
| **일일** | 진행 중인 업무마다 비중 0·1·2·3을 고르면 그날 근무 MD가 비중대로 자동 분배됩니다. 업무별 메모, 하루 메모, 저장 전 공수 미리보기. |
| **주간** | 그 주의 `해야 할 일 / 실제 달성한 일 / 차주 업무`를 자동 정리. 제목은 `2026-10-26 ~ 2026-10-30 개발1팀`. 전체·섹션별 원클릭 복사. |
| **월별** | 서비스/고객사 태그 또는 항목을 골라 기간별 월 MD를 조회·복사. 월간 요약 표(근무일, 연차 사용, 항목별 비율)는 엑셀용 탭 구분으로 복사. |
| **업무** | 업무 항목 등록, 대기→진행 중→완료 상태 관리, 연차·반차·휴일 미리 등록, 팀명 설정. |

> 완료한 항목은 일일 목록에서 사라지지만 기록은 남아서 월별 공수에 계속 잡힙니다.

## 기술 스택과 선택 이유

| 영역 | 선택 | 이유 |
|---|---|---|
| 백엔드 | Java 21, Spring Boot 3, Spring Data JPA, Gradle | 업무에서 쓰는 스택. MD 계산·보고 판정을 서버에 두고 테스트하기 좋음 |
| 인증 | Spring Security + JWT(httpOnly 쿠키) | 지금은 계정 하나지만 팀원 추가 시 구조 변경 불필요. 토큰을 JS에서 읽을 수 없게 해 XSS에 강함 |
| DB | PostgreSQL 16 + Flyway | 월별 집계 쿼리에 강하고, 스키마 변경 이력을 코드로 관리 |
| 프론트 | Vite + React + TypeScript + Tailwind CSS v4 | 빠른 개발 루프, 타입 안전, 디자인 토큰을 CSS 변수로 관리 |
| 서버 상태 | TanStack Query | 저장 후 관련 화면(주간·월별 등)을 무효화로 일관되게 갱신 |
| 모션 | Motion (motion/react) | 세그먼트 선택 이동, 토스트, 다이얼로그 전환 |
| 테스트 | JUnit 5 + Testcontainers(실제 Postgres), Vitest + Testing Library | 집계·판정 로직을 실제 DB로 검증 |
| 실행 | Docker Compose (postgres + backend + nginx) | 한 줄로 전체 실행. nginx가 `/api`를 프록시해 같은 출처로 쿠키 동작 |

## 실행하기

### Docker Compose (전체)

Docker Desktop이 필요합니다.

```bash
docker compose up --build -d
```

브라우저에서 <http://localhost:8081> 을 열고 가입하면 시작됩니다. 종료는 `docker compose down`(데이터는 볼륨에 남음).

- 웹(8081)과 DB(5432)는 **내 PC(127.0.0.1)에서만** 접속됩니다. 같은 네트워크의 다른 기기에서는 열리지 않습니다.
- `JWT_SECRET`을 지정하지 않으면 실행할 때마다 무작위 키를 만들어서, 재시작하면 다시 로그인해야 합니다. 로그인을 유지하려면 32바이트 이상의 값을 지정하세요: `JWT_SECRET=... docker compose up -d`
- `SIGNUP_ENABLED=false`로 회원가입을 막을 수 있습니다.
- 외부에 공개할 계획이라면 DB 비밀번호와 쿠키 `Secure` 설정(HTTPS)부터 바꿔야 합니다.

### 개발 모드

```bash
docker compose up -d postgres          # DB만
cd backend && ./gradlew bootRun        # http://localhost:8080 (JDK 21)
cd frontend && npm install && npm run dev   # http://localhost:5173 (/api는 8080으로 프록시)
```

## 핵심 로직

- **MD 분배 (`MdAllocator`)**: 그날 근무 MD(근무 1.00, 반차 0.50, 연차·휴일 0)를 비중 비율로 0.01 단위로 나눕니다. 나눈 뒤 남는 0.01은 **나머지가 큰 항목부터** 배분해서(동률이면 앞 항목) **하루 합계가 항상 정확히 근무 MD**가 됩니다. 1·1·1이면 0.34 / 0.33 / 0.33. 원가분석 합계가 어긋나지 않게 하려는 설계이고, 프론트의 미리보기(`previewAllocation`)도 같은 알고리즘입니다.
- **주간 보고 판정 (`WeeklyReportService`)**: 시작일·완료일·생성일로 "그 주 시점"을 재구성하므로 지난 주를 열어도 당시 기준으로 나옵니다.
  - 해야 할 일 = 금요일까지 만들어졌고 월요일 이전에 완료되지 않은 항목
  - 차주 업무 = 금요일 시점에 완료되지 않은 항목
  - 실제 달성한 일 = 월~금 요일별로 그날 기록한 항목(MD, 메모, 완료 표시)과 연차·반차
  - 항목별 "계획 섹션에 표시" 옵션으로 회의·기타 같은 상시 업무는 달성한 일에만 나오게 합니다.
- **공수 조회 (`EffortReportService`)**: 태그(현재 태그 기준) 또는 항목 단위로 기간 내 모든 달을 채워서 반환합니다. 기록 없는 달은 `0.00MD`.
- 날짜는 서버 설정과 무관하게 항상 `Asia/Seoul` 기준입니다.

## 테스트

Docker가 켜져 있어야 합니다(Testcontainers가 Postgres를 띄웁니다).

```bash
cd backend && ./gradlew test        # 73개: MD 분배, 일일 저장, 주간 판정, 공수 집계, 인증
cd frontend && npx vitest run       # 28개: 날짜/MD 유틸, 각 화면 동작
```

## 폴더 구조

```
backend/   Spring Boot (auth, item, day, report 패키지) + Flyway 마이그레이션
frontend/  Vite + React (features/daily, weekly, monthly, items, auth)
docs/superpowers/specs/   설계 문서
docs/superpowers/plans/   구현 계획
```

## 범위 밖 (의도적으로 뺀 것)

M/M 환산(회사 기준 미정이라 MD만), 팀 기능(팀원 초대·열람 — 스키마만 대비), 공휴일 자동 반영, 다크 모드, 배포 자동화.
