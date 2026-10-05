# PLAN — 이용 내역 "대여중" 필터에 RETURNING(연체) 대여 포함

> 대상: `SCR-APP-HISTORY-001` 필터 동작 수정(기존 기능 수정, Phase 6에서 이미 DONE 처리된 화면).
> 근거: 사용자 요청("연체중이면 대여중 필터에서 같이 보이게 해줘") + 대화 중 확인:
> - 연체는 백엔드에 별도 `Rental.Status`가 아니라 `dueAt` 기준으로 매번 계산됨(`RentalOverdueCalculator`,
>   `backend/.../rental/RentalOverdueCalculator.java`). 저장된 상태는 `REQUESTED/ACTIVE/RETURNING/
>   COMPLETED/LOST/CANCELLED/FAILED` 뿐이라, 연체된 `ACTIVE` 대여는 이미 "대여중" 필터(`status=ACTIVE`
>   쿼리)에 포함되어 있다.
> - 실제로 빠지는 케이스는 **`RETURNING`(반납 시도 중) 상태이면서 연체된 대여** — 상태 Chip 규칙(§9.2,
>   `HistoryItemCard.tsx`)은 `ACTIVE`·`RETURNING`을 같은 "대여중/연체중" 묶음으로 표시하지만, 필터 탭은
>   서버에 `status=ACTIVE`만 보내 `RETURNING` 항목이 제외됨.
> - 사용자가 프론트 전용 방식으로 결정: 백엔드(`RentalAppService.buildFilter`)는 건드리지 않고, 프론트에서
>   "대여중" 필터 선택 시 `status=ACTIVE`·`status=RETURNING` 두 번 조회해 병합.

## 구현 범위

- `src/api/historyApi.ts`: `getRentalHistory`의 `status` 파라미터 타입을 `HistoryStatusFilter`(문서화된
  RENTAL-002 필터 enum, UI 탭 전용)에서 백엔드가 실제로 받는 `RentalStatus | 'ALL'`로 넓힌다. `RENTAL-002`
  API 계약 자체(§5.6 문서 값)는 변경하지 않음 — 이 함수는 그 계약이 커버하지 않는 `RETURNING` 조회에도
  재사용되는 저수준 함수가 됨을 주석으로 명시.
- `src/screens/SCR-APP-HISTORY-001/HistoryScreen.tsx`: `filter === 'ACTIVE'`일 때 `useQueries`로
  `status: 'ACTIVE'`·`status: 'RETURNING'` 두 쿼리를 병렬 조회 후 `items`를 `rentedAt` 내림차순으로
  병합. 그 외 필터는 기존과 동일하게 단일 `useQuery`. 로딩·에러·새로고침(Pull-to-Refresh) 상태는 두
  쿼리를 합쳐서 판단(하나라도 pending이면 로딩, 하나라도 error면 에러 배너, 새로고침은 둘 다 refetch).
- 새 API·타입·상태값 추가 없음(`RETURNING`은 이미 `src/types/rental.ts`의 `RENTAL_STATUS`에 존재).

## 변경하지 않는 것

- 백엔드(`RentalAppService.java`) — 이번엔 프론트 전용 해결.
- `HistoryStatusFilter`/`FILTERS`/`EMPTY_MESSAGE` — 탭 자체는 여전히 5개(전체/대여중/반납완료/분실·파손/
  취소됨) 그대로, "대여중" 탭 뒤의 조회 로직만 바뀜.
- 페이지네이션(`nextCursor`) — 기존에도 미사용 상태 그대로 유지(이번 Phase 범위 아님).
