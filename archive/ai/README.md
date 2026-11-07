# AI

RentalTrack의 AI는 이미지 손상 판정을 핵심으로 두지 않습니다.

## Phase 4

현재 구현 방향은 지점별 상품 수요 예측과 재고 보충 의사결정 지원입니다.

```text
Rental history
→ Historical Average baseline
→ Granite TTM-R3 zero-shot
→ chronological evaluation
→ demand forecast
→ replenishment rule
→ operator decision
```

구현 위치:

```text
ai/demand_forecasting/
```

자세한 실행 방법은 [demand_forecasting/README.md](./demand_forecasting/README.md)를 참고합니다.

## 원칙

- 합성 데이터는 실제 수요라고 표현하지 않습니다.
- TTM-R3는 먼저 zero-shot으로 검증합니다.
- Historical Average를 반드시 baseline으로 둡니다.
- 랜덤 train/test split 대신 시간순 평가를 사용합니다.
- AI는 수요를 예측하고, 보충 수량은 별도 rule로 계산합니다.
- 운영자가 최종 판단합니다.
