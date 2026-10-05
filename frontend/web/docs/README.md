# API 스펙 사본

## `api-docs.json`

백엔드가 자동 생성한 OpenAPI 3.1 스펙의 **스냅샷**입니다.

- 출처: `GET http://i15a201.p.ssafy.io:8080/v3/api-docs` (Swagger UI 뒤에 있는 기계용 원본)
- 받은 시각: **2026-08-03**
- 받은 방법: 읽기 전용 GET **1회**. 로그인·업무 데이터 조회 없음

## 왜 파일로 두나

**EC2에 붙지 않고 계약을 대조하기 위해서입니다.** 관리자 웹은 목업 모드로 개발하는 게
기본이라(`.env.development` 의 `VITE_USE_MOCK_DATA=true`), 서버에 요청을 보내지 않고도
경로·파라미터·응답 필드가 맞는지 확인할 수 있어야 합니다. 이 파일이 그 기준입니다.

Swagger UI 페이지(`/swagger-ui/index.html`)를 열어도 같은 내용을 볼 수 있지만, 그건 매번
서버를 호출합니다. 대조는 이 파일로 하세요.

## 갱신

백엔드가 API를 바꾸면 **낡습니다.** 최신이 필요하면 그때 다시 1회 받아 덮어쓰고, 위
"받은 시각"을 고치세요. 미구현이라 지금 스펙에 없는 것: `GET /auth/me`, `POST /auth/logout`
(다음 주 보완 예정).

## 대조 결과 (2026-08-03)

- 관리자 API 8개 경로·필드 일치
- `GET /inspections` 의 query `aiResult` enum 은 **`NORMAL|DAMAGED|UNCERTAIN` 셋뿐**입니다.
  응답의 AI 결과 네 개(`FAILED` 포함)와 값 집합이 다릅니다 — 헷갈리기 쉬운 자리라
  `InspectionListPage` 의 필터 선택지에서 `FAILED` 를 뺐습니다.
- `InspectionDecisionRequest.reasonCode` 는 스펙상 optional 인데 우리 코드는 필수로 다룹니다
  (서버가 조건부 검증하는 것으로 보임). 백엔드 확인 필요.
