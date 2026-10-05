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

### Next domain
- [ ] Member
- [ ] ConditionRecord / ConditionImage
- [ ] DamageRecord
- [ ] ReturnInspection
- [ ] Member FK migration
- [ ] Phase 1 전체 schema integration test

## Phase 2 — Core Web Flow

- [ ] 상품/지점 탐색
- [ ] 대여 신청
- [ ] 인수 처리
- [ ] 대여 전 상태 확인
- [ ] 반납
- [ ] 대여 후 상태 기록
- [ ] 검수
- [ ] 사용자 대여 이력

## Phase 3 — Quality

- [ ] API validation
- [ ] 핵심 상태 전이 테스트
- [ ] DB 제약조건 테스트
- [ ] 통합 테스트
- [ ] 사용자/운영자 권한 분리
- [ ] 이미지 저장소 분리

## Phase 4 — Data / AI

- [ ] 수요 예측용 데이터셋 정의
- [ ] Baseline 모델
- [ ] ML 모델 비교
- [ ] 품절 위험 계산
- [ ] 운영자 재고 보충 추천
- [ ] 예측 성능 및 운영 지표 평가
