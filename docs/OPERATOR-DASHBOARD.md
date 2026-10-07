# Operator AI Dashboard

RentalTrack 운영자가 수요 예측 결과를 조회하고 재고 보충 추천을 승인/기각하는 웹 화면입니다.

## Data flow

```text
TTM-R3 / Historical Average
→ model_run
→ demand_forecast
→ inventory_recommendation
→ GET /api/v1/operator/ai/dashboard
→ React Operator Dashboard
→ ACCEPTED / DISMISSED
```

## 화면 구성

### 운영 요약

- 향후 예측 수요
- 예측 대상 지점·카테고리 series 수
- 미처리 recommendation 수
- HIGH 위험 recommendation 수
- 추가 배치 권장 수량 합계

### 모델 비교

같은 datasetVersion에서 실행된 성공 모델의 MAE / RMSE를 비교합니다.

현재 실험에서는 Historical Average와 Granite TTM-R3 zero-shot을 같은 holdout 구간에서 비교합니다.

### 위험도 분포

OPEN recommendation을 HIGH / MEDIUM / LOW로 집계합니다.

### 우선 검토 목록

위험도가 높은 recommendation을 먼저 보여주며 동일 위험도에서는 추가 권장 수량이 큰 항목을 우선합니다.

운영자는 각 항목에서:

```text
승인 → ACCEPTED
기각 → DISMISSED
```

를 선택할 수 있습니다.

## 인증

운영자 화면은 이메일/비밀번호 로그인으로 JWT access token을 발급받습니다.

```text
POST /api/v1/auth/login
→ BCrypt password 검증
→ JWT access token
→ Authorization: Bearer <token>
→ Backend에서 ACTIVE / DB role 재확인
```

현재 Frontend는 access token을 localStorage에 보관합니다. 역할은 Frontend 값이 아니라 Backend에서 조회한 DB role을 기준으로 판단합니다.

## 개발 실행

Backend:

```bash
cd backend
./gradlew bootRun
```

Frontend:

```bash
cd frontend/web
npm install
npm run dev
```

Vite 개발 서버는 `/api`를 `http://localhost:8080`으로 proxy합니다.

## 현재 범위

Dashboard의 recommendation 승인은 운영 판단 기록만 남깁니다. 상품 재배치 작업을 자동 수행하지 않습니다.
