# RentalTrack Roadmap

## Phase 0 — Skeleton

- [x] Web 중심 프로젝트로 재정의
- [x] Spring Boot + MySQL 기반 최소 Backend 구성
- [x] React + TypeScript 기반 최소 Frontend 구성
- [x] 기존 AIoT 의존성을 새 구조에서 제거
- [x] 도메인 초안 문서화

## Phase 1 — Domain

### Core domain
- [x] RentalLocation ERD / Entity
- [x] ProductCategory ERD / Entity
- [x] RentalProduct ERD / Entity
- [x] RentalContract ERD / Entity
- [x] Flyway V1 core schema

### History domain
- [x] Member ERD / Entity
- [x] Member ↔ RentalContract FK
- [x] Flyway V2 member schema
- [x] ConditionRecord ERD / Entity
- [x] ConditionImage ERD / Entity
- [x] Flyway V3 condition schema
- [x] DamageRecord ERD / Entity
- [x] Flyway V4 damage schema
- [x] ReturnInspection ERD / Entity
- [x] Flyway V5 inspection schema

### Phase 1 verification
- [x] 전체 Flyway migration 실행 검증
- [x] Entity ↔ Flyway `ddl-auto=validate` 검증
- [x] 핵심 DB 제약조건 integration test
- [x] GitHub Actions 자동 검증
- [x] GitLab CI 설정 파일 구성

## Phase 2 — Core Web Flow

- [x] 상품/지점 탐색
- [x] 대여 신청
- [x] 인수 처리
- [x] 대여 전 상태 확인
- [x] 반납
- [x] 대여 후 상태 기록
- [x] 기존/신규 손상 비교
- [x] 반납 검수
- [x] 사용자 대여 이력

## Phase 3 — Quality

- [x] API validation
- [x] 핵심 상태 전이 테스트
- [x] 서비스 계층 동시성 테스트
- [x] 사용자/운영자 권한 분리
- [x] 이미지 저장소 분리

## Phase 4 — Data / AI

- [x] 수요 예측용 데이터셋 정의
- [x] Baseline 모델
- [x] TTM-R3 zero-shot 모델 연결 및 비교
- [x] 품절 위험 계산
- [x] 운영자 재고 보충 추천
- [x] 시간순 평가 및 MAE/RMSE 비교
- [x] model_run 운영자 조회 API
- [x] demand_forecast 운영자 조회 API
- [x] inventory_recommendation 운영자 조회 API

- [x] inventory_recommendation ACCEPTED / DISMISSED 운영자 의사결정 API
