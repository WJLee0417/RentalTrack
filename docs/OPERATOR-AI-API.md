# Operator AI Query API

Phase 4에서 생성한 `model_run`, `demand_forecast`, `inventory_recommendation` 결과를 Spring Boot 운영자 API로 조회합니다.

모든 API는 현재 Phase 3의 임시 인증 경계인 `X-Actor-Id`를 사용하며, DB의 `member_account.role = OPERATOR`인 사용자만 접근할 수 있습니다.

## Base path

```text
/api/v1/operator/ai
```

## Model runs

```text
GET /api/v1/operator/ai/model-runs
GET /api/v1/operator/ai/model-runs/{modelRunId}
```

Optional filters:

```text
runType        BASELINE | TTM_ZERO_SHOT
status         RUNNING | SUCCEEDED | FAILED
datasetVersion exact match
page           default 0
size           default 20, max 100
```

목록은 `startedAt DESC, id DESC` 순서입니다.

## Demand forecasts

```text
GET /api/v1/operator/ai/forecasts
GET /api/v1/operator/ai/forecasts/{forecastId}
```

Optional filters:

```text
modelRunId
locationId
categoryId
page
size
```

응답에는 모델 실행 정보와 지점/카테고리 이름을 같이 반환합니다.

## Inventory recommendations

```text
GET /api/v1/operator/ai/recommendations
GET /api/v1/operator/ai/recommendations/{recommendationId}
```

Optional filters:

```text
status      OPEN | ACCEPTED | DISMISSED
riskLevel   LOW | MEDIUM | HIGH
locationId
categoryId
page
size
```

응답에는 다음 운영 판단 정보를 같이 반환합니다.

```text
predictedDemand
availableQuantity
safetyStock
recommendedAdditionalQuantity
riskLevel
reason
status
```

## 권한

```text
USER
→ 403 FORBIDDEN

OPERATOR
→ 조회 허용
```

역할 자체를 request parameter나 header에서 전달받지 않고, `X-Actor-Id`로 조회한 Member의 DB role을 기준으로 판단합니다.

## Recommendation decision

운영자는 OPEN 상태의 recommendation을 최종 판단할 수 있습니다.

```text
PATCH /api/v1/operator/ai/recommendations/{recommendationId}/decision
```

Request:

```json
{
  "decision": "ACCEPTED"
}
```

또는:

```json
{
  "decision": "DISMISSED"
}
```

허용되는 최종 판단은 `ACCEPTED`, `DISMISSED` 두 값뿐입니다.

상태 전이:

```text
OPEN
├─→ ACCEPTED
└─→ DISMISSED
```

이미 최종 판단된 recommendation은 다시 변경할 수 없습니다.

```text
ACCEPTED → DISMISSED  X
DISMISSED → ACCEPTED  X
```

중복 또는 경합하는 운영자 판단을 직렬화하기 위해 recommendation 조회에 `PESSIMISTIC_WRITE` lock을 적용합니다.

결정이 완료되면 `decidedAt`을 기록하고 변경된 recommendation 전체 응답을 반환합니다.

오류 정책:

```text
OPEN을 decision으로 요청
→ 400 BAD_REQUEST

이미 확정된 recommendation 재결정
→ 409 INVALID_STATE

존재하지 않는 recommendation
→ 404 NOT_FOUND

USER 접근
→ 403 FORBIDDEN
```

## 현재 범위

현재 API는 추천을 승인하거나 기각하는 운영자 판단을 기록합니다.
승인 이후 상품 재배치 자체를 자동 실행하지는 않습니다.


## Dashboard aggregate

운영자 화면의 초기 로딩에서 여러 조회 API를 조합하지 않도록 Dashboard 집계 API를 제공합니다.

```text
GET /api/v1/operator/ai/dashboard
```

응답은 다음 정보를 한 번에 제공합니다.

```text
datasetVersion

latestModel
- modelRunId
- modelName
- modelVersion
- runType
- mae
- rmse
- completedAt

forecast
- seriesCount
- predictedDemand

recommendations
- openCount
- highRiskOpenCount
- mediumRiskOpenCount
- lowRiskOpenCount
- recommendedAdditionalQuantity

modelComparison[]
priorityRecommendations[]
```

`modelComparison`은 최근 성공 실행의 datasetVersion과 같은 성공 모델 실행을 비교합니다.

`priorityRecommendations`는 OPEN recommendation 중 위험도와 추가 필요 수량을 기준으로 우선 검토할 상위 5건을 반환합니다.

성공한 model_run이 아직 없다면 404 대신 빈 Dashboard 응답을 반환해 Frontend가 초기 상태를 표현할 수 있도록 합니다.
