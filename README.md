# RentalTrack

이동 중 갑자기 필요한 생활용품을 주변 대여 지점에서 찾고 대여·반납할 수 있는 웹 기반 렌탈 서비스입니다.

RentalTrack은 대여 건마다 상품의 상태·사진·손상 이력을 분리해 기록하고, 이후 축적되는 이용 데이터를 활용해 지점별 상품 수요와 재고 부족 가능성을 예측하는 방향으로 확장합니다.

## 현재 단계

현재 브랜치는 기존 Billisan AIoT 구조를 걷어내고 RentalTrack Web v1의 뼈대를 다시 세우는 단계입니다.

### MVP 핵심 흐름

```text
주변 대여 지점/상품 탐색
→ 대여 신청
→ 상품 인수
→ 대여 전 상태 확인
→ 이용
→ 반납
→ 대여 후 상태 기록
→ 손상 이력 확인
→ 반납 검수
```

### 핵심 도메인

- Member
- RentalLocation
- ProductCategory
- RentalProduct
- RentalContract
- ConditionRecord
- ConditionImage
- DamageRecord
- ReturnInspection

### AI 확장 방향

대여 이력이 충분히 쌓인 뒤 다음 데이터를 활용해 `지점 × 상품` 단위 수요를 예측합니다.

- 시간대 / 요일
- 대여·반납 이력
- 현재 재고
- 날씨
- 지점 특성

예측 결과는 자동 의사결정이 아니라 운영자의 재고 보충·상품 배치 판단을 돕는 정보로 사용합니다.

## 기술 스택

- Frontend: React, TypeScript, Vite
- Backend: Java 21, Spring Boot, Spring Data JPA
- Database: MySQL, Flyway
- Infra: Docker Compose
- AI/Data: Python 기반 수요 예측 모듈 예정

## Repository

```text
RentalTrack/
├─ backend/
├─ frontend/
│  └─ web/
├─ ai/
├─ docs/
├─ compose.yaml
└─ README.md
```

## 실행

환경변수 예시를 복사합니다.

```bash
cp .env.example .env
```

Backend와 MySQL:

```bash
docker compose up --build
```

Frontend:

```bash
cd frontend/web
npm install
npm run dev
```

자세한 설계 방향은 `docs/`를 참고합니다.
