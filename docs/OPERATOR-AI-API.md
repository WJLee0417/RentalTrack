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

## 현재 범위

이 API는 조회 전용입니다.

`inventory_recommendation.status`를 `ACCEPTED / DISMISSED`로 변경하는 운영자 의사결정 API는 다음 단계에서 별도로 추가합니다.
