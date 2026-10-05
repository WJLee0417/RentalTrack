# PLAN — SCR-KSK-COMMON (Loading / Error / Success / Face Guide Overlay)

> 대상 Phase: PHASE.md "Phase 1 (SCR-KSK-COMMON)" 중 남은 항목
> (KioskLayout·공통 Button·메인 화면 UI·공통 색상/Typography는 `2026-07-28-SCR-KSK-MAIN-001.md`에서 이미 완료)
> 근거 문서: docs/09-screen-flow.md §3.2(AUTH), §3.5(ERROR) / docs/PROJECT_GUIDE.md §6(상태 코드), §8(진행 원칙)

---

## 1. 구현 목표

- Phase 2/3(대여·반납·인증 흐름)에서 재사용할 **범용 프레젠테이션 컴포넌트 4종**을 만든다.
- 특정 Screen ID의 도메인 로직(세션, API, 상태머신)은 다루지 않는다 — 이번 Phase는 "껍데기"만 만들고, 실제 배선은 Phase 2/3에서 각 화면이 이 컴포넌트를 가져다 쓰면서 진행한다.
- 09번 문서의 `SCR-KSK-ERROR-001`(UNSETTLED_BLOCKED 등)이나 `SCR-KSK-RENT-001/RETURN-001`의 `COMPLETED` Variant처럼 **특정 도메인 상태에 종속된 화면은 이번 범위가 아니다.** 이번엔 그 화면들이 공통으로 쓸 뼈대(메시지 + 액션 버튼 슬롯)만 만든다.

---

## 2. 만들 파일 목록

| 구분 | 경로 |
|---|---|
| 신규 | `src/components/common/LoadingScreen.tsx` |
| 신규 | `src/components/common/ErrorScreen.tsx` |
| 신규 | `src/components/common/SuccessScreen.tsx` |
| 신규 | `src/components/common/FaceGuideOverlay.tsx` |

---

## 3. 각 컴포넌트의 역할과 API

### `LoadingScreen`
PROJECT_GUIDE.md §8 원칙 5 "모든 비동기 처리 단계에 로딩/처리 중 표시 필수"에 대응하는 범용 로딩 화면. `KioskLayout`으로 감싸고 스피너 + 안내 메시지만 표시한다.

```tsx
interface LoadingScreenProps {
  message: string
}
```

- 진행률 표시(`progressMode`)는 넣지 않는다 — 실제 AI 진행률 제공 가능 여부가 `DEC-KSK-005 미정`이라 09번 문서 기준으로도 `INDETERMINATE`(불확정) 스피너만 다룰 수 있음.

### `ErrorScreen`
09번 문서 §3.5 `SCR-KSK-ERROR-001`의 공통 뼈대. 메시지 + 액션 버튼(기본 라벨 "홈으로")만 제공하고, 어떤 조건(`UNSETTLED_BLOCKED`/`ACTIVE_RENTAL_EXISTS`/`ACTIVE_RENTAL_NOT_FOUND`)에서 띄울지는 다루지 않는다(Phase 2/3 몫).

```tsx
interface ErrorScreenProps {
  message: string
  actionLabel?: string   // 기본값 '홈으로'
  onAction: () => void
}
```

- **자동 복귀 타이머는 넣지 않는다.** 09번 문서 §7 `WF-CHANGE-017`(5초 고정 제거) + §6 `DEC-SF-011`(자동 복귀 시간 미정) 근거로, 확인 버튼 클릭으로만 닫히게 한다.

### `SuccessScreen`
09번 문서의 `RENT-001/COMPLETED`, `RETURN-001/COMPLETED` 등이 공통으로 쓸 뼈대. 체크 아이콘 + 메시지 + 확인 버튼(기본 라벨 "확인").

```tsx
interface SuccessScreenProps {
  message: string
  actionLabel?: string   // 기본값 '확인'
  onAction: () => void
}
```

- 대여/반납 완료 화면 전용 필드(대여 시각·슬롯 번호 등)는 넣지 않는다 — 이건 Phase 2/3에서 이 컴포넌트를 감싸는 화면이 `children`이나 별도 props로 추가할 몫.
- 위와 동일하게 자동 복귀 타이머 없음.

### `FaceGuideOverlay`
09번 문서 §3.2 `SCR-KSK-AUTH-001 FACE_CAPTURE`의 "얼굴 가이드" 요소. 카메라 프리뷰 위에 얹는 **순수 시각 오버레이**(SVG 타원 가이드라인)만 만든다. 실제 카메라 스트림·얼굴 검출 로직은 포함하지 않는다(PROJECT_GUIDE.md §5.5 "얼굴 검출·임베딩은 담당하지 않는다").

```tsx
interface FaceGuideOverlayProps {
  className?: string
}
```

- `position: absolute` 기반으로, 부모 컨테이너(카메라 프리뷰 영역) 위에 겹쳐 쓰는 걸 전제로 한다.

---

## 4. 테스트 방법 (수동 확인 기준)

각 컴포넌트를 임시로 `App.tsx`에서 번갈아 렌더링해 육안 확인 후, 확인이 끝나면 `App.tsx`는 다시 `MainScreen`을 렌더링하는 상태로 되돌린다(실제 화면 배선은 Phase 2/3 몫이므로 이번 Phase에서 라우팅을 새로 만들지 않는다).

1. `LoadingScreen message="처리 중입니다"` — 스피너 애니메이션 동작, 레이아웃 깨짐 없는지 확인
2. `ErrorScreen message="..." onAction={...}` — 버튼 클릭 시 콜백 호출 확인, 자동으로 사라지지 않는지 확인
3. `SuccessScreen message="..." onAction={...}` — 동일
4. `FaceGuideOverlay`를 임시 카메라 자리(회색 박스) 위에 겹쳐서 위치·비율 확인
5. `npm run build`, `npm run lint` 통과 확인

---

## 5. 예상 리스크

1. **자동 복귀 시간(`DEC-SF-011`)·재시도 횟수(`DEC-SF-010`) 미정** — `ErrorScreen`/`SuccessScreen`에 타이머·재시도 카운터를 넣지 않고 명시적 액션 버튼만 제공하는 것으로 리스크를 회피한다. 추후 결정되면 컴포넌트 props를 확장해야 한다.
2. **실제 AI 진행률 제공 가능 여부(`DEC-KSK-005`) 미정** — `LoadingScreen`은 확정 진행률 바를 만들지 않고 불확정 스피너만 사용한다.
3. **Face Guide 시각 형태 근거 부족** — 09번 문서에 가이드 모양(원/타원/사각)의 정확한 스펙이 없어 실측 와이어프레임(`Billisan_Kiosk_Camera.png`) 기반 근사치로 만든다. 최종 디자인 확정 시 조정 필요.
4. **컴포넌트 재사용 시점 불일치**: 이번에 만드는 뼈대의 실제 props 형태가 Phase 2/3 착수 시점에 화면별 요구사항과 안 맞아 수정이 필요할 수 있다(예: `SuccessScreen`에 슬롯 번호 표시용 `children` 슬롯 추가 등).

---

## 6. 이번 Phase에서 하지 않을 일

- `SCR-KSK-ERROR-001`의 3개 Variant(`UNSETTLED_BLOCKED`/`ACTIVE_RENTAL_EXISTS`/`ACTIVE_RENTAL_NOT_FOUND`) 배선 — 세션·활성 대여 조회 로직이 필요해 Phase 2/3 영역
- `SCR-KSK-RENT-001`/`RETURN-001`의 `COMPLETED` Variant 실제 배선
- 카메라 스트림 연동, 얼굴 검출/매칭 로직
- 라우팅/화면 전환 로직 (여전히 미설치 상태, 새 라이브러리 도입은 별도 승인 필요)
- 자동 복귀 타이머, 재시도 횟수 제한 구현
