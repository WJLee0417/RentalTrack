# PLAN: 키오스크 공통 비주얼 리프레시 (Primary 컬러 · 배경 그라디언트 · KioskLayout 구분 제거)

> 근거: 사용자 지시(2026-08-03) — "프라이머리 컬러 FFD700으로 변경 / 배경을 상단 화이트→하단 FDFAEA 그라디언트로 / KioskLayout의 header-content 구분을 없애고 싶다"
> 확인 완료(AskUserQuestion, 2026-08-03):
> - 그라디언트 하단 색상은 **FDFAEA**(신규 지정값)로 확정 — 기존 코드/PROJECT_GUIDE.md 실측값 `#FDFAE9`와 마지막 한 자리 다름을 인지한 상태에서 새 값 채택.
> - "구분 제거"는 **KioskLayout 자체를 수정해 전체 화면 공통 적용**하는 것으로 확정(메인 화면 한정 아님) — KioskLayout을 사용하는 모든 화면(11개 스크린 컴포넌트)에 영향.
> 대응 문서: PROJECT_GUIDE.md §14.1 디자인 시스템 컬러 팔레트 — 이번 변경으로 실측 기재값과 코드가 달라지므로 6절 리스크에 반영.
> 아직 승인 전 — **이 문서 승인 전에는 코드를 작성하지 않는다.**

---

## 1. 구현 목표

1. Primary 컬러 토큰을 `#fdcb24` → `#FFD700`으로 변경
2. 전체 화면 배경을 흰색(상단) → `#FDFAEA`(하단) 그라디언트로 변경, `--color-kiosk-bg` 토큰 값도 `#FDFAEA`로 동기화
3. `KioskLayout`의 header/content 시각적 경계(콘텐츠 영역만 별도 배경색 + 상단 라운드 60px + 그림자로 "카드" 처럼 보이던 부분)를 제거해 헤더~콘텐츠가 하나의 연속된 그라디언트 배경을 공유하도록 변경

이번 변경은 색상 토큰 + `KioskLayout` 구조 조정만 다루며, 각 화면 내부 로직·상태 관리는 건드리지 않는다.

---

## 2. 영향 범위

`--color-primary`, `--color-kiosk-bg`는 Tailwind `@theme` 토큰이라 값만 바꾸면 사용하는 모든 곳에 자동 반영된다(개별 파일 수정 불필요). 현재 사용처:

| 토큰 | 사용 파일 |
|---|---|
| `bg-primary`/`text-primary`/`border-primary` 등 | `RentalCompleteScreen`, `ReturnCompleteScreen`, `UmbrellaGuideScreen`, `LoadingScreen`, `KioskLayout`, `IconBadge`, `StepIndicator`, `RentalBlockModal`, `GuideContent`, `Button`, `ActionCard`, `InfoNoticeBar` |
| `bg-kiosk-bg` | `KioskLayout`(제거 대상), `IconBadge`, `ActionCard` |

`KioskLayout`을 사용하는 화면(구조 변경 영향, 11개): `FaceAuthGuideScreen`, `AuthSuccessScreen`, `RentalCompleteScreen`, `PickupSlotScreen`, `ReturnCompleteScreen`, `UmbrellaGuideScreen`, `LoadingScreen`, `ErrorScreen`, `MainScreen`, `ReturnSlotScreen`, `CameraCaptureScreen`. `AuthScreen`/`RentFlow`/`ReturnFlow`는 라우팅용 컨테이너라 직접 `KioskLayout`을 쓰지 않고 위 화면들을 렌더링하므로 간접 영향.

---

## 3. 만들거나 수정할 파일 목록

| 구분 | 파일 | 내용 |
|---|---|---|
| 수정 | `src/index.css` | `--color-primary: #FFD700`, `--color-kiosk-bg: #FDFAEA` |
| 수정 | `src/components/layout/KioskLayout.tsx` | 그라디언트 배경을 최상위 컨테이너로 이동, content 래퍼의 `bg-kiosk-bg` / `rounded-t-[60px] rounded-b-none` / `shadow-[...]` 제거 |

다른 화면 파일은 토큰 자동 반영이므로 수정하지 않는다.

---

## 4. 각 파일의 역할

### `src/index.css` (수정)
- `@theme` 블록의 `--color-primary`를 `#fdcb24` → `#FFD700`, `--color-kiosk-bg`를 `#fdfae9` → `#FDFAEA`로 변경
- `body`의 `background`는 건드리지 않는다(현재 `background: var(#ffffff);`는 유효하지 않은 CSS라 사실상 무시되고 있는데, 이번 배경 변경을 `KioskLayout` 루트에서 처리하므로 실제로는 계속 영향이 없다 — 6절 리스크에 별도 기재, 이번 PLAN 범위에서 고치지 않음)

### `src/components/layout/KioskLayout.tsx` (수정)
- 현재 최상위 `<div className="flex h-screen flex-col">`에 그라디언트 배경 클래스(`bg-gradient-to-b from-white to-[#FDFAEA]`)를 추가
- `<header>`는 그대로 투명 유지(부모의 그라디언트가 자연스럽게 비쳐 보임)
- `<main>` 안쪽 content 래퍼 `div`에서 `bg-kiosk-bg`, `rounded-t-[60px] rounded-b-none`, `shadow-[0_-4px_8px_rgba(120,120,120,0.12)]`를 제거 — `flex min-h-0 w-full flex-1 flex-col items-center overflow-hidden`과 `fullBleed` 여부에 따른 `gap-14 p-14`는 레이아웃 목적이라 유지
- 결과적으로 헤더 영역과 콘텐츠 영역이 배경·경계 없이 하나로 이어짐

---

## 5. 테스트 방법 (수동 확인 기준)

1. `npm run build`, `npm run lint` 통과 확인
2. `MainScreen`: 상단 헤더부터 하단까지 흰색→`#FDFAEA` 그라디언트가 끊김 없이 이어지는지, 대여 카드(Primary) 색상이 `#FFD700`으로 바뀌었는지 육안 확인
3. `KioskLayout`을 쓰는 다른 화면(예: `LoadingScreen`, `ErrorScreen`, `RentalCompleteScreen`) 최소 2~3개를 열어 이전에 보이던 "상단 라운드+그림자 카드" 경계가 사라졌는지, 그라디언트가 자연스럽게 유지되는지 확인
4. 뒤로가기 버튼(`bg-primary`), `StepIndicator`, `Button` 등 Primary 컬러를 쓰는 다른 요소들도 `#FFD700`으로 갱신됐는지 확인
5. 반응형/실기 테스트는 하지 않는다(PHASE.md·PROJECT_GUIDE.md 기준상 실기 검증은 별도 게이트)

---

## 6. 예상 리스크

- **PROJECT_GUIDE.md §14.1 실측값과의 불일치**: 문서는 `#FDFAE9`(2026-07-27 픽셀 샘플링 실측)를 "확정"으로 기재하고 있는데, 이번 변경은 사용자 지시로 `#FDFAEA`를 새로 채택한다. 코드와 문서가 갈라지므로, 이번 작업 완료 후 PROJECT_GUIDE.md §14.1 값도 갱신할지 별도로 확인이 필요하다(이번 PLAN에는 문서 갱신 포함 안 함).
- **`body`의 잘못된 CSS(`background: var(#ffffff)`)는 그대로 둠**: 현재도 사실상 미적용 상태라 이번 변경과 충돌하지는 않지만, 잠재적으로 다른 버그의 원인일 수 있다. 이번 스코프에서는 손대지 않고 리스크로만 기록.
- **KioskLayout 구조 변경의 넓은 영향 범위**: 11개 화면이 공유하는 컴포넌트라, 화면별로 `bg-kiosk-bg` 카드 배경을 전제로 만들어진 하위 요소(흰 카드 위에 얹힌 아이콘/텍스트 대비 등)가 있다면 새 그라디언트 배경 위에서 대비가 부족해 보일 수 있다. 이번 PLAN은 `KioskLayout` 자체의 배경·경계만 바꾸고 하위 화면 각각의 카드/대비 스타일은 조정하지 않으므로, 확인 중 어색한 화면이 나오면 별도로 알리고 후속 조정 여부를 논의한다.

---

## 7. 이번 PLAN에서 하지 않을 일

- 각 화면 내부 컴포넌트(카드, 배지 등)의 개별 디자인 조정 — 토큰 변경으로 자동 반영되는 색상 외에는 손대지 않음
- PROJECT_GUIDE.md §14.1 컬러 팔레트 표 갱신 — 별도 확인 후 진행
- `body`의 잘못된 CSS 수정
- `MainScreen.tsx`의 레이아웃/카드 구조 자체 변경 — 이번 요청은 배경·경계 스타일이 대상이며, 카드 배치 등은 범위 밖(필요하면 별도 PLAN)
