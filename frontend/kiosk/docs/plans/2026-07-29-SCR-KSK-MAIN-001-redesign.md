# PLAN: SCR-KSK-MAIN-001 — 메인 화면 리디자인

> 티켓: **71**(`S15P11A201-71-fe-kiosk-common` 후속) — 단, 브랜치는 현재 `S15P11A201-75-fe-kiosk-rental`에서 그대로 작업하기로 결정(사용자 확인, 2026-07-29). 커밋 메시지는 `S15P11A201-71` 접두사로 구분.
> 근거: 사용자 제공 목업 이미지(2026-07-29) — 대여 3개/반납 가능 우산함 2개 통계 바, 카드형 대여·반납 버튼, 하단 3열 안내 바
> 대응 문서: `docs/09-screen-flow.md` §3.1 SCR-KSK-MAIN-001 (단, 이번 리디자인 중 문서에 없는 새 요소 있음 — 6절 리스크 참고)
> 이전 PLAN(`2026-07-28-SCR-KSK-MAIN-001.md`, `2026-07-28-SCR-KSK-MAIN-001-mock-integration.md`)의 후속 — 기존 Mock/스토어 구조(`kioskMainStore`, `stationApi`)는 유지하고 확장만 한다.
> 아직 승인 전 — **이 문서 승인 전에는 코드를 작성하지 않는다.**

---

## 1. 구현 목표

메인 화면을 목업 기준으로 전면 리디자인한다. 기존 기능(대여 가능 수량 조회, 재고 0개 시 대여 비활성, 대여/반납 진입)은 그대로 유지하면서 시각 구조만 바꾼다.

- 상단 통계 바: 대여 가능 수량 + **반납 가능한 우산함 수량**(신규 데이터)
- 카드형 대여/반납 버튼(아이콘·제목·부제·화살표·배경 일러스트)
- 하단 3열 정적 안내 바(학생 인증 안내 / 운영시간·문의 / 주의사항)

---

## 2. 기존 파일 재사용 검토

| 파일 | 재사용 여부 | 이유 |
|---|---|---|
| `KioskLayout.tsx` | ✅ 그대로 재사용 | 헤더(로고·날짜시간)는 목업과 동일. 본문은 `fullBleed` prop으로 받아서 `MainScreen`이 직접 레이아웃 제어(현재 `justify-center gap-14 p-20` 중앙정렬 방식이 새 디자인의 상단정렬 구조와 안 맞아서, `KioskLayout` 자체는 안 건드리고 `fullBleed`로 우회) |
| `Button.tsx` | ❌ 새 카드 버튼엔 미사용 | 아이콘 원+제목+부제+화살표+배경 일러스트 구조가 기존 pill 버튼과 완전히 다름. 신규 `ActionCard.tsx` 필요. `Button.tsx` 자체는 다른 화면(Error/Success/Loading 액션)에서 계속 쓰이므로 삭제하지 않음 |
| `UmbrellaIcon.tsx`, `ReturnIcon.tsx` | ✅ 재사용 | 새 카드의 아이콘 원 안 + 배경 큰 일러스트에 그대로 사용(같은 아이콘을 크기·투명도만 다르게 두 번 렌더링) |
| `Badge.tsx` | ⚠️ 재사용 안 함, 삭제도 안 함 | 목업에 "재고 없음" 배지가 안 보임(3절 참고). 지금 `Badge`는 `MainScreen`에서만 쓰이고 있어서(`grep` 확인 완료) 리디자인 후 쓰는 곳이 없어지지만, 다른 화면에서 나중에 필요할 수 있어 파일은 남겨둠 |
| `AlertCircleIcon.tsx` | ✅ 재사용 | 하단 안내 바 3번째 항목(경고) 아이콘과 목업의 원형 느낌표 아이콘이 동일해 보임 |

---

## 3. 만들거나 수정할 파일 목록

| 구분 | 파일 |
|---|---|
| 신규 | `src/components/common/ActionCard.tsx` |
| 신규 | `src/components/common/InfoNoticeBar.tsx` |
| 신규 | `src/components/icons/UserIcon.tsx` |
| 신규 | `src/components/icons/ClockIcon.tsx` |
| 신규 | `src/components/icons/ArrowRightIcon.tsx` |
| 수정 | `src/types/slot.ts` |
| 수정 | `src/api/stationApi.ts` |
| 수정 | `src/store/kioskMainStore.ts` |
| 수정 | `src/screens/SCR-KSK-MAIN-001/MainScreen.tsx` |

---

## 4. 각 파일의 역할

### `src/components/common/ActionCard.tsx` (신규)
- props: `variant`(`primary`/`secondary`), `icon`, `title`, `subtitle`, `onClick`, `disabled`
- `primary`: 노란 그라데이션 배경 + 흰 아이콘 원 + 오른쪽에 같은 아이콘의 크고 옅은 배경 버전 + 원형 화살표 버튼
- `secondary`: 흰 배경 + 회색 톤 아이콘/화살표(지금 `대여` 카드와 `반납` 카드 색 대비와 동일)
- `disabled`일 때: `secondary`와 같은 무채색 톤으로 전환 + 클릭 비활성(목업에 비활성 상태 스샷이 없어서 이렇게 가정 — 8절 리스크 참고)

### `src/components/common/InfoNoticeBar.tsx` (신규)
- 3개 항목을 세로 구분선으로 나눠 보여주는 정적 안내 바
- 항목 배열을 `MainScreen`에서 하드코딩해 props로 넘김(아이콘, 제목, 부제) — 재사용 가능성이 낮아 항목 자체는 이 컴포넌트 안에 고정하지 않음

### `src/components/icons/UserIcon.tsx`, `ClockIcon.tsx`, `ArrowRightIcon.tsx` (신규)
- 기존 아이콘들과 동일한 패턴(`SVGProps<SVGSVGElement>`, `currentColor` 상속)의 인라인 SVG

### `src/types/slot.ts` (수정)
- `StationSummary`에 `returnableSlotCount: number` 필드 추가 — **문서 근거 없음(6절 리스크)**, 목업 반영을 위한 임시 확장

### `src/api/stationApi.ts` (수정)
- `SCENARIOS`(`AVAILABLE`/`EMPTY`/`STALE`) 각각에 `returnableSlotCount` 값 추가

### `src/store/kioskMainStore.ts` (수정)
- `returnableSlotCount: number | null` 상태 필드 추가, `fetchStationSummary` 액션이 같이 채움
- `selectMainScreenVariant`(대여 가능 여부 판단)는 **변경하지 않음** — 반납 버튼은 지금처럼 재고와 무관하게 항상 활성 유지(09번 문서 §3.1 "얼굴 인증 전에는 활성 대여 유무를 알 수 없다" 원칙 유지, `returnableSlotCount`는 표시 전용)

### `src/screens/SCR-KSK-MAIN-001/MainScreen.tsx` (수정)
- 상단 통계 바(대여/반납 수량 2개), `ActionCard` 2개(대여/반납), `InfoNoticeBar` 순서로 재구성
- `KioskLayout fullBleed`로 감싸고 내부에서 직접 세로 배치·여백 관리

---

## 5. 상태 관리 방식

기존 `kioskMainStore`를 확장만 한다(새 스토어 안 만듦).

```
kioskMainStore (확장):
  usableSlotCount: number | null       (기존)
  returnableSlotCount: number | null   (신규)
  isLoading / isStale / error          (기존, 두 수량에 공통 적용)
  fetchStationSummary()                (기존 로직 그대로, 응답에 returnableSlotCount만 추가로 저장)
```

`variant`(대여 버튼 활성/비활성)는 지금처럼 `usableSlotCount`/`isStale`/`error`로만 결정하고 `returnableSlotCount`는 관여하지 않는다 — 반납 버튼은 항상 활성 유지 정책(기존 결정 유지).

---

## 6. Mock 데이터 구조

```ts
interface StationSummary {
  usableSlotCount: number
  returnableSlotCount: number   // 신규, 근거 문서 없음
  observedAt: string
  stale: boolean
}
```

`SCENARIOS`도 목업 값(대여 3개/반납 2개)을 `AVAILABLE`에 반영:
```
AVAILABLE: { usableSlotCount: 3, returnableSlotCount: 2, stale: false }
EMPTY:     { usableSlotCount: 0, returnableSlotCount: 2, stale: false }
STALE:     { usableSlotCount: 3, returnableSlotCount: 2, stale: true }
```

---

## 7. 테스트 방법 (수동 확인 기준)

1. `npm run build`, `npm run lint` 통과 확인
2. 기본 상태(`AVAILABLE`): 통계 바에 "대여 가능한 우산 3개" / "반납 가능한 우산함 2개" 표시, 두 카드 모두 클릭 가능한 형태로 렌더링되는지 확인
3. `EMPTY`로 바꿔서: 대여 카드가 비활성 스타일로 바뀌고 클릭이 막히는지, 반납 카드는 그대로 활성인지 확인
4. `STALE`로 바꿔서: 대여 카드가 보수적으로 비활성 처리되는지 확인(기존 정책 유지)
5. 로딩 중(`LoadingScreen`) → 완료 전환이 매끄러운지 확인
6. 실제 브라우저에서 로고가 목업과 같은 에셋인지 육안 대조(1절 근거 참고, 다르면 알려주기로 함)
7. 반응형까지는 확인 안 함(키오스크 고정 해상도 전제, PROJECT_GUIDE.md 기준)

---

## 8. 예상 리스크

- **`returnableSlotCount` 문서 근거 없음**: PROJECT_GUIDE.md §5.4 `KSK-STATION-001` 설명에는 이 필드가 명시돼 있지 않다. Mock 단계라 우선 반영하지만, 실제 API 스키마 확정 시 필드명·존재 여부가 바뀔 수 있다.
- **비활성(재고 0) 상태의 카드 디자인 미확인**: 제공된 목업은 정상 상태(3개/2개) 하나뿐이라, 대여 카드가 비활성일 때 정확히 어떤 톤으로 바뀌어야 하는지 근거가 없다. 이번 PLAN은 `secondary`(반납 카드와 같은 무채색) 스타일로 가정한다 — 실제 디자인과 다를 수 있어 확인 후 조정 필요.
- **"재고 없음" 문구 표시 방식 불확실**: 기존엔 `Badge`로 "우산 재고 없음" 텍스트를 보여줬는데, 새 목업엔 그 자리가 없다. 이번 PLAN은 카드 자체의 비활성 스타일로만 표현하고 별도 텍스트 배지는 없앤다 — 사용자 피드백에 따라 되돌릴 수 있음.
- **로고 에셋 일치 여부 미확인**: 1절에 적었듯 SVG 코드만으로는 목업 로고와 `logo-umbrella.svg`가 같은지 확신할 수 없다.
- **하단 안내 바 문구가 실제 정책과 다를 수 있음**: "문의 051-510-1234", "운영시간 09:00-21:00"은 PROJECT_GUIDE.md §9.2 `DEC-KSK-001`에 따라 아직 확정 안 된 예시값 취급이다 — 목업에 나온 값을 그대로 쓰되, 실제 값이 아닐 수 있음을 인지하고 진행.

---

## 9. 이번 PLAN에서 하지 않을 일

- 반납 카드에 `returnableSlotCount === 0`일 때 비활성화하는 로직 — 기존 정책(반납은 항상 활성) 유지, 바꾸지 않음
- 실제 `KSK-STATION-001` API 연동 — Mock만 사용
- 운영시간·전화번호를 서버 값으로 대체하는 작업 — `DEC-KSK-001` 미확정 상태라 정적 문구 유지
- `RETURN_DISABLED_WIREFRAME_ONLY` variant 구현 — 09번 문서 기준 제거 권장, 계속 미구현
- `Badge.tsx` 삭제 — 사용처가 없어져도 파일은 남겨둠(9절 3번째 항목과 별개로, 확실하지 않은 삭제는 하지 않는다는 원칙)
