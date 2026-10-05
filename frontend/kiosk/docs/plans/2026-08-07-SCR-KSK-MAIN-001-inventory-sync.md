# PLAN: 대여/반납 완료 응답의 inventory로 메인 화면 재고 동기화

- 작성일: 2026-08-07
- 관련 Screen ID: SCR-KSK-MAIN-001(재고 표시), SCR-KSK-RENT-001, SCR-KSK-RETURN-001(현재 Phase)
- 근거: 사용자가 대화 중 붙여넣은 임베디드팀 문서 발췌("4. Pi 쪽 처리", "5. 키오스크로 나가는 최종 형태")
  - **주의**: 이 발췌는 임베디드팀이 "구현 완료·참고용"이라고 설명한 스펙 문서이며, 실제 캡처한 Pi 응답
    JSON(userName/studentId 필드 때처럼 `mosquitto_sub`로 실측 확인한 것)은 아니다. 문서 자체에도
    "6. 테스트 방법"으로 사후 검증 절차가 안내돼 있다 — 즉 이 필드 도입 시점엔 아직 실측 검증 전이다.
  - 문서상 스프링이 값을 못 주면 `inventory: null`이 온다고 명시돼 있으므로 프론트는 반드시 null-safe로 처리한다.

## 배경

`KIOSK.RENT.RESULT` / `KIOSK.RETURN.RESULT` payload에 다음 필드가 추가된다:

```json
"inventory": { "rentableSlotCount": 7, "emptyReturnSlotCount": 3, "totalSlotCount": 10 } // 또는 null
```

현재 `kioskMainStore`의 재고(`usableSlotCount`/`returnableSlotCount`)는 `KSK-STATION-001`(`getStationSummary`) 조회로만 갱신된다.
대여/반납이 완료돼 홈으로 돌아왔을 때 재조회 전까지는 방금 바뀐 재고가 반영되지 않는 gap이 있다 — 이 gap을
없애기 위해 완료 RESULT의 `inventory`를 받는 즉시 스토어를 갱신한다.

## 변경 파일

1. **`src/types/piProtocol.ts`**
   - `RentResult`, `ReturnResult`에 `inventory: { rentableSlotCount: number; emptyReturnSlotCount: number; totalSlotCount: number } | null` 필드 추가.
   - 출처·미실측 상태를 밝히는 TEMP 주석 추가(기존 `userName`/`studentId` 주석 톤과 동일하게, 단 "실측 확인"이 아니라 "임베디드 문서 예시, 실측 전"이라고 정확히 구분해서 표기).

2. **`src/store/kioskMainStore.ts`**
   - 새 액션 `applyInventorySnapshot(inventory: { rentableSlotCount: number; emptyReturnSlotCount: number } | null)` 추가.
   - `inventory`가 `null`이면 아무것도 바꾸지 않는다(스프링 미연동 상황을 재고 0으로 오인해 `RENT_DISABLED_NO_STOCK`을 잘못 띄우지 않도록).
   - `inventory`가 있으면 `usableSlotCount`/`returnableSlotCount`를 그 값으로 덮고 `isStale=false`, `error=null`로 전환(Pi가 중계한 권위값이므로).
   - `totalSlotCount`는 현재 어떤 화면도 쓰지 않으므로 스토어에 저장하지 않고 버린다(안 쓰는 필드를 상태로 만들지 않음).

3. **`src/screens/SCR-KSK-RENT-001/RentFlow.tsx`**
   - `rentResult.terminalStatus === 'SUCCEEDED'` 분기에서 `useKioskMainStore.getState().applyInventorySnapshot(rentResult.inventory)` 호출 추가.

4. **`src/screens/SCR-KSK-RETURN-001/ReturnFlow.tsx`**
   - `finalizeIfReady()`의 `terminalStatus === 'SUCCEEDED'` 분기에서 동일하게 `applyInventorySnapshot(returnResult.inventory)` 호출 추가.

## 범위 밖(하지 않는 것)

- `KSK-STATION-001` 응답 자체의 `stale` 계산 로직 변경 없음 — 이번 변경은 완료 RESULT 수신 시점의 "즉시 반영"만 다룬다.
- `totalSlotCount` 관련 UI 추가 없음(요청받지 않음).
- Pi 쪽 `EDGE-DEVICE-001`/`EDGE-HEALTH-001` 관련 변경 없음(프론트 범위 아님).

## 리스크

- 이 스펙이 아직 실측 검증 전이므로, 필드명이나 null 여부가 실제와 다를 수 있다. 실 대여/반납 1회 완주로
  `inventory` 수신 여부·수치를 확인하기 전까지는 `PROVISIONAL`로 취급하고, 실측되면 이 PLAN과 타입 주석을 갱신한다.
