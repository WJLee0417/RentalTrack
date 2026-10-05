# PLAN — SCR-APP-STATION-001 (커스텀 캠퍼스 지도)

> 대상 Phase: PHASE.md "Phase 7"(2026-08-05 재정렬로 토스 결제보다 먼저 순서를 가져옴)
> 근거: 사용자가 직접 작성한 스펙("커스텀 캠퍼스 지도(Map) 기능 구현", 2026-08-05) + 이어진 확인
> 질문 4개의 답변. `DEC-SF-004`(지도 SDK·위치 권한) RESOLVED — `docs/PROJECT_GUIDE.md` §8·§13 반영 완료.
> `app-screen-flow.md` §2.3의 Variant(`LOCATION_PERMISSION_DENIED`/`MAP_SDK_ERROR` 등)는 실제
> GPS/지도 SDK를 전제로 한 노션 원문 그대로라, 이번 결정(가상 캠퍼스·위치 권한 불필요)과 개념적으로
> 안 맞는다는 점을 알린다 — 노션 문서보다 이번 사용자 확정 스펙을 우선한다.

---

## 1. 구현 목표

- 실제 지도 API(네이버·카카오·Google Maps) 없이, SVG 기반 가상 캠퍼스 지도 위에 대여소 마커를 표시한다.
- Pinch Zoom(1~3배) + Pan(이동 범위 제한)을 지원한다.
- 마커는 우산 개수에 따라 색이 바뀌고(3개 이상 초록/1~2개 노랑/0개 빨강), 선택 시 확대+그림자+애니메이션으로 강조된다.
- 마커를 탭하면 Bottom Sheet(직접 구현)로 대여소 이름·남은 우산 개수를 보여준다 — 이번 Phase에서는
  Bottom Sheet가 대여소 상세 정보의 전부이며, 별도 `SCR-APP-STATION-DETAIL-001` 화면은 만들지 않는다
  (사용자 확인).
- 신규 라이브러리 설치 없음 — `react-native-svg`/`react-native-gesture-handler`/`react-native-reanimated`
  모두 기존 설치분 재사용.

## 2. 만들 파일 목록

| 구분 | 경로 | 역할 |
|---|---|---|
| 신규 | `src/assets/campus-map.svg` | 가상 캠퍼스 지도 배경(추상적 플레이스홀더 — 사용자 확인, 건물 느낌의 단순 도형) |
| 신규 | `src/types/station.ts` | `Station` 타입(`id, name, x, y, availableCount`) — 12-R에 `STATION-001` API 계약이 없어 잠정 구조임을 주석으로 명시 |
| 신규 | `src/api/stationApi.ts` | Mock — `GET /stations` 응답 형태를 흉내(실제 API 계약 미확정, `inquiryApi.ts`와 동일하게 잠정 구조로 표시). Mock 대여소 5~8개를 캠퍼스 곳곳에 배치(사용자 확인) |
| 신규 | `src/screens/SCR-APP-STATION-001/CampusMap.tsx` | SVG 지도 + Pinch/Pan 컨테이너. 마커도 이 컴포넌트 안, 같은 Zoom 트랜스폼 레이어에 렌더링(스펙 §9 "SVG와 마커는 같은 Zoom Container") |
| 신규 | `src/screens/SCR-APP-STATION-001/StationMarker.tsx` | 마커 1개 — 우산 개수별 색상, 선택 시 scale-up + shadow |
| 신규 | `src/screens/SCR-APP-STATION-001/StationBottomSheet.tsx` | 직접 구현 Bottom Sheet(새 라이브러리 없음) — reanimated로 open 320ms/close 240ms(INT-APP-005 규격 재사용) |
| 신규 | `src/screens/SCR-APP-STATION-001/hooks/useCampusMapGesture.ts` | Pinch+Pan 제스처 → scale/translateX/translateY(reanimated shared value) 커스텀 훅. 배율 1~3 클램프, 지도가 화면 밖으로 완전히 안 나가게 이동 범위 클램프 |
| 수정 | `src/screens/SCR-APP-STATION-001/MapScreen.tsx` | 자리표시자 → `CampusMap` + `StationBottomSheet` 조립, `selectedStationId`만 화면 레벨 state로 관리(스펙 §7) |

## 3. 좌표·줌 시스템 설계

- 마커 좌표는 0~1 비율(`x`, `y`)로 저장, 렌더링 시 `left = mapWidth * x`, `top = mapHeight * y`로 변환
  (지도 컨테이너의 `onLayout`으로 측정한 실제 픽셀 크기 기준 — 화면 크기가 달라도 상대 위치 동일).
- 줌/팬 상태(`scale`, `translateX`, `translateY`)는 React state가 아니라 reanimated `useSharedValue`로
  `CampusMap`/`useCampusMapGesture` 내부에서만 관리 — 리렌더 없이 `useAnimatedStyle`로 트랜스폼만 갱신
  (스펙 §7 "확대/축소 상태는 지도 컴포넌트 내부에서 관리" + §9 "불필요한 리렌더링 최소화").
- `Gesture.Pinch()`와 `Gesture.Pan()`을 `Gesture.Simultaneous()`로 묶어 동시 인식.
- 배율 클램프: `Math.min(3, Math.max(1, scale))`.
- 이동 범위 클램프: 현재 배율에서 지도 콘텐츠 크기가 컨테이너보다 큰 만큼만 이동 허용(지도가 화면 밖으로
  완전히 사라지지 않게).
- Double Tap Zoom은 스펙대로 구현하지 않음.

## 4. 마커 색상·강조 규칙

- `availableCount >= 3` → 초록(`#22C55E`, Tailwind green-500)
- `availableCount === 1 | 2` → 노랑(`#EAB308`, Tailwind yellow-500 — 앱 브랜드 primary(`#FFD700`)와 헷갈리지
  않도록 다른 톤 사용)
- `availableCount === 0` → 빨강(`#EF4444`, Tailwind red-500)
- 선택된 마커: `scale` 애니메이션(reanimated) + `shadow` 스타일로 강조

## 5. Bottom Sheet 설계

- 새 라이브러리 없이 `Animated.View`(reanimated) + `Pressable` 배경 오버레이로 직접 구현
- open 320ms / close 240ms(기존 `INT-APP-005` 규격 그대로 재사용)
- 표시 내용: 대여소 이름, 남은 우산 개수, 간단 설명(선택 — Mock 데이터에 짧은 문구 필드 추가)
- `selectedStationId`가 `null`이 아니면 표시, 배경 탭 또는 닫기로 `null`로 되돌림

## 6. `stationApi.ts` / `types/station.ts` 설계

```ts
// types/station.ts
export interface Station {
  id: number
  name: string
  x: number // 0~1
  y: number // 0~1
  availableCount: number
  description?: string
}
```

- `stationApi.ts`는 `MOCK_STATIONS: Station[]` 5~8개(캠퍼스 곳곳에 흩어진 가상 위치, 초록/노랑/빨강
  케이스가 골고루 섞이도록 구성) + `getStations(): Promise<Station[]>`
- 12-R에 `STATION-001` API 계약 자체가 없어(§4 P1·NOT_ACTIVE_P0) `inquiryApi.ts`처럼 "잠정 구조"임을
  파일 상단 주석으로 명시 — 나중에 실제 `GET /stations` 계약이 정해지면 응답 형태만 맞추면 되도록 함

## 7. 확인된 결정 사항 (2026-08-05 사용자 확인)

- SVG 캠퍼스 지도: 실제 캠퍼스 도면이 아니라 건물 느낌의 단순 도형으로 직접 생성
- Bottom Sheet: 새 라이브러리 없이 직접 구현
- Bottom Sheet가 대여소 상세 정보의 전부 — 별도 `SCR-APP-STATION-DETAIL-001` 화면 없음
- Mock 대여소 5~8개, 캠퍼스 곳곳에 배치

## 8. 하지 않는 일

- 실제 지도 SDK 연동(네이버/카카오/Google Maps) — `DEC-SF-004` 결정에 따라 이번 Phase에서 아예 안 함
- 위치 권한 요청(`expo-location` 등) — 가상 지도라 실제 GPS 불필요
- 별도 `SCR-APP-STATION-DETAIL-001` 화면 — Bottom Sheet로 대체(사용자 확인)
- Double Tap Zoom
- 실제 `GET /stations` API 연동 — Mock만

## 9. 테스트 방법

1. 지도 탭 진입 → SVG 캠퍼스 지도 + 마커 5~8개 표시 확인
2. 손가락으로 Pinch(확대/축소, 1~3배 제한) + Pan(이동, 화면 밖으로 완전히 안 나감) 확인
3. 마커 우산 개수별 색상(초록/노랑/빨강) 확인
4. 마커 탭 → 선택 애니메이션(확대+그림자) + Bottom Sheet 표시(이름·개수·설명) 확인
5. Bottom Sheet 닫기(배경 탭/닫기 버튼) → `selectedStationId` 초기화 확인
6. `npx tsc --noEmit` 통과 확인
