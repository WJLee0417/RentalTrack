# RentalTrack Architecture v0.1

## 현재 목표 구조

```text
Mobile / Desktop Web
        │
        │ HTTP REST
        ▼
Spring Boot Backend
        │
        ├─ Member
        ├─ Location
        ├─ Product
        ├─ Rental
        ├─ Condition
        ├─ Damage
        └─ Inspection
        │
        ▼
      MySQL
```

## 현재 범위에서 제거한 구조

기존 Billisan의 다음 구성은 RentalTrack Web v1의 설계 범위가 아닙니다.

- Raspberry Pi
- Jetson Orin
- MQTT / Mosquitto
- ZeroMQ
- 물리 슬롯 센서
- 얼굴 인증
- 키오스크
- pgvector 얼굴 DB
- 장치 snapshot / reconciliation
- 날씨 챗봇

## AI 연결 방향

Web 서비스의 대여 이력을 먼저 축적한 뒤 별도 Python 분석 모듈을 추가합니다.

```text
Rental / Inventory Data
        │
        ▼
Demand Dataset
        │
        ▼
Demand Prediction
        │
        ▼
Stock-out Risk
        │
        ▼
Operator Recommendation
```

초기에는 Backend와 AI를 강하게 결합하지 않습니다.
