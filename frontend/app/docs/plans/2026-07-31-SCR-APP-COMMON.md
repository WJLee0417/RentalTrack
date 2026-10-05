# PLAN — SCR-APP-COMMON (App 공통 UI·내비게이션 뼈대)

> 대상 Phase: PHASE.md "Phase 1 (SCR-APP-COMMON)"
> 근거 문서: docs/PROJECT_GUIDE.md §3(기술 스택), §4(화면 카탈로그), §9(공통 규칙) / docs/app-screen-flow.md §1
> 이미 완료: Expo 프로젝트 초기화(SDK 54), React Navigation·Zustand 설치(2026-07-31)
>
> ⚠️ **정정(2026-07-31, 같은 날 추가)**: 사용자가 실제 앱 와이어프레임(PDF)을 공유한 뒤, 하단 탭 구성을
> **홈 / 지도 / 내역조회 / 마이페이지**로 변경했다(챗봇은 탭이 아니라 홈 화면의 플로팅 버튼). 아래 §3·§5·§6의
> "홈/이용내역/마이페이지/챗봇" 서술은 이 정정 이전 상태이며, 실제 코드는 이미 새 구성으로 반영되어 있다.
> 상세는 `PROJECT_GUIDE.md` §12 참고.

---

## 1. 구현 목표

- 이후 모든 화면(Phase 2~)이 재사용할 **레이아웃·공통 컴포넌트·내비게이션 뼈대**를 만든다.
- 특정 화면의 실제 내용(로그인 폼, 대여 내역 목록 등)은 다루지 않는다 — 이번엔 "껍데기"만 만들고, 각 화면의 실제 UI·API 연동은 Phase 2 이후에 진행한다.
- 하단 탭 4개(홈·이용내역·마이페이지·챗봇)에 **빈 화면(자리표시자)만** 연결해서, 탭을 눌렀을 때 화면이 바뀌는 것까지만 확인한다.

---

## 2. 만들 파일 목록

| 구분 | 경로 | 역할 |
|---|---|---|
| 신규 | `src/theme/colors.ts` | 색상 토큰 |
| 신규 | `src/theme/typography.ts` | 글자 크기·굵기 토큰 |
| 신규 | `src/components/layout/Screen.tsx` | Safe Area 대응 공통 화면 래퍼 |
| 신규 | `src/components/common/Button.tsx` | 공통 버튼 |
| 신규 | `src/components/common/TextField.tsx` | 공통 입력창 |
| 신규 | `src/components/common/LoadingScreen.tsx` | 공통 로딩 화면 |
| 신규 | `src/components/common/ErrorBanner.tsx` | 공통 오류 배너 |
| 신규 | `src/navigation/RootNavigator.tsx` | 하단 탭 내비게이션 뼈대 |
| 신규 | `src/screens/SCR-APP-HOME-001/HomeScreen.tsx` | 자리표시자(다음 Phase에서 실제 구현) |
| 신규 | `src/screens/SCR-APP-HISTORY-001/HistoryScreen.tsx` | 자리표시자 |
| 신규 | `src/screens/SCR-APP-MYPAGE-001/MyPageScreen.tsx` | 자리표시자 |
| 신규 | `src/screens/SCR-APP-CHATBOT-001/ChatbotScreen.tsx` | 자리표시자(정정 후: 탭 아님, 홈 화면 플로팅 버튼으로 나중에 연결 예정) |
| 신규(정정 후 추가) | `src/screens/SCR-APP-STATION-001/MapScreen.tsx` | 자리표시자(지도 탭) |
| 수정 | `App.tsx` | 기본 템플릿 문구 대신 `RootNavigator` 렌더링 |

---

## 3. 각 요소의 역할과 결정 사항

### 색상·타이포그래피 토큰 (`theme/`)
> ⚠️ **확인 필요**: PROJECT_GUIDE.md §3에 App 전용 색상 hex 값은 없음("문서에 없음/미확인"). 06번 §2.6은 "브랜드 토큰을 App·Kiosk·Admin이 공유한다"고 되어 있어서, **Kiosk에서 이미 실측 확정한 팔레트**(Primary Gold `#FFD700`, 크림 배경 `#FDFAE9`, 다크 네이비 `#11182A` 등, `frontend/kiosk` 디자인 시스템 참고)를 그대로 가져와 쓰는 걸 제안한다. 다른 색을 원하시면 알려주세요 — 이 부분만 결정되면 바로 반영합니다.

### `Screen` (레이아웃)
Kiosk의 `KioskLayout`과 같은 역할 — 모든 화면을 이걸로 감싸서 노치·상태바 영역을 자동으로 피하게 한다(`react-native-safe-area-context`의 `SafeAreaView` 활용).

```tsx
interface ScreenProps {
  children: ReactNode
}
```

### `Button` / `TextField`
Kiosk의 공통 Button과 같은 목적. 최소 props(`label`/`onPress`/`disabled` 등, `TextField`는 `value`/`onChangeText`/`placeholder`/`secureTextEntry`)만 두고, 화면별 커스텀은 Phase 2 이후 필요할 때 확장한다.

### `LoadingScreen` / `ErrorBanner`
API 호출 중 로딩 표시, 실패 시 오류 안내용 범용 컴포넌트. PROJECT_GUIDE.md §9.4 "모든 비동기 처리 단계에 로딩 표시 필수, 기술 오류·stack trace를 사용자 메시지에 남기지 않는다" 원칙을 따른다.

### `RootNavigator` (하단 탭)
React Navigation의 `createBottomTabNavigator`로 탭 4개 구성: **홈 / 이용내역 / 마이페이지 / 챗봇**.

- PROJECT_GUIDE.md §4의 P0 확정 화면 4개(`SCR-APP-HOME-001`, `SCR-APP-HISTORY-001`, `SCR-APP-MYPAGE-001`, `SCR-APP-CHATBOT-001`)만 탭으로 노출한다.
- `SCR-APP-STATION-001`(지도, P1)·`SCR-APP-NOTIFICATION-001`·`SCR-APP-INQUIRY-001`(둘 다 보류)는 탭에 넣지 않는다 — 아직 `DECISION_REQUIRED` 상태라 임의로 화면 자리를 만들지 않는다.
- 로그인 화면(`SCR-APP-AUTH-001`)은 탭이 아니라 **탭 내비게이션 진입 전 단계**라, 이번 Phase에서는 아직 연결하지 않는다(Phase 2에서 "로그인 전/후" 분기 추가 예정). 지금은 탭 화면들이 바로 보이는 임시 상태.

### Zustand 스토어
> ⚠️ **범위 축소**: PHASE.md에 "Zustand 스토어 초기 설정"이라고 되어 있었지만, 지금 시점에 실제로 저장할 상태(로그인 세션 등)가 아직 없어서 — 빈 스토어 파일을 미리 만들지 않는다. 라이브러리 설치만 이번 Phase에서 끝내고, 실제 스토어(`useAuthStore` 등)는 그 상태가 필요해지는 Phase(로그인 = Phase 2)에서 만든다. 안 쓰는 빈 파일을 미리 만들지 않기 위함.

---

## 4. 테스트 방법 (수동 확인 기준)

1. `npm start` 실행 → Expo Go로 스캔
2. 하단에 탭 4개(홈/이용내역/마이페이지/챗봇)가 보이는지 확인
3. 각 탭을 눌러서 해당 자리표시자 화면(예: "홈 화면 — 준비 중")으로 바뀌는지 확인
4. 노치가 있는 폰 화면 기준으로 상단 내용이 상태바에 가려지지 않는지 확인(Safe Area)
5. `Button`/`TextField`를 자리표시자 화면 중 하나에 임시로 렌더링해 눌림/입력 동작 확인 후 제거

---

## 5. 예상 리스크

1. **색상 팔레트 미확정** — 위 §3에서 Kiosk 팔레트 재사용을 제안했지만 확정 전까지는 승인 대기.
2. **챗봇을 별도 탭으로 둘지, Home 진입 버튼으로만 둘지** — 09번 문서 원문은 "Home에서 실제 진입"이라고 서술하지만, 하단 탭에 노출해도 문서 원칙(P0 확정 화면이라는 것)과 충돌하지 않는다고 판단해 탭으로 진행한다. 나중에 다르게 바꿔도 구조상 어렵지 않음.
3. **로그인 전/후 내비게이션 분기 구조** — 이번 Phase는 탭만 만들고 "비로그인 상태" 분기는 안 만든다. Phase 2에서 로그인 화면을 만들 때 이 구조를 어떻게 감쌀지 다시 설계가 필요할 수 있음.

---

## 6. 이번 Phase에서 하지 않을 일

- 로그인 화면 실제 구현, 로그인 여부에 따른 화면 분기
- 각 탭 화면의 실제 내용(API 연동, 목록 표시 등)
- Zustand 실제 스토어 파일 생성(§3 참고, 다음 Phase로 이월)
- 지도·알림·문의 화면(P1/보류 상태라 이번 범위 아님)
