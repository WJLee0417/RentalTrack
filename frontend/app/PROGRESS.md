# PROGRESS

## 현재 src 폴더 구조 (2026-08-09 기준, Phase 7(`SCR-APP-STATION-001`, 카카오맵 커스텀 타일셋으로 재구현) 완료 · Phase 8(Toss 결제 연동)이 다음 착수 대상. 그 사이 실제 Spring 연동 완료(`docs/plans/2026-08-06-api-integration.md`) + 이미 완료된 Phase(로그인·알림)에 대한 추가 UX 개선(비밀번호 표시/지우기 아이콘, 알림 읽음 상태 영속화, 로컬 알림 배너) 반영 — 아래 변경 이력 참고)

```
frontend/app/
├── App.tsx                          GestureHandlerRootView + SafeAreaProvider + QueryClientProvider +
│                                     RootNavigator 렌더링
├── index.ts                         global.css import + registerRootComponent
├── global.css                       Tailwind 지시자(@tailwind base/components/utilities)
├── tailwind.config.js               NativeWind preset + 색상 토큰(primary/secondary/background/text/
│                                     text-school 등)
├── babel.config.js                  babel-preset-expo + nativewind/babel
├── metro.config.js                  withNativeWind(getDefaultConfig(...))
├── nativewind-env.d.ts              NativeWind className 타입 지원
├── AGENTS.md                        Expo 템플릿 기본 생성 파일 — SDK 버전별 문서 확인 안내(유지)
├── .prettierrc.json / .prettierignore / .vscode/settings.json   Prettier + 저장 시 자동 포맷
├── src/
│   ├── api/
│   │   ├── client.ts                공용 axios 인스턴스 — X-Request-Id/Authorization 자동 첨부,
│   │   │                            에러 코드를 Error.message로 정규화, 401 시 setUnauthorizedHandler 콜백
│   │   ├── authApi.ts               AUTH-001(로그인)/AUTH-002(로그아웃) 실 연동
│   │   ├── homeApi.ts               HOME-001 실 연동
│   │   ├── weatherApi.ts            WEATHER-001 Mock 유지(DEC-SF-003 미확정, provider "UNDECIDED" 고정)
│   │   ├── userApi.ts               USER-001 실 연동(studentNumber는 실제 응답에 없어 표시 제거, §12 참고)
│   │   ├── rentalApi.ts             RENTAL-001 실 연동
│   │   ├── settlementApi.ts         SETTLEMENT-001 실 연동
│   │   ├── inquiryApi.ts            문의 제출 Mock 유지(INQUIRY-001 미확정 — 잠정 구조, types/inquiry.ts 참고)
│   │   ├── chatbotApi.ts            CHATBOT-001 실 연동
│   │   ├── historyApi.ts            RENTAL-002 실 연동(status/cursor/size 쿼리 전달)
│   │   ├── alertApi.ts              ALERT-001 실 연동({items:[]} 래퍼 언래핑)
│   │   └── stationApi.ts            STATION-001 Mock 유지(잠정 구조 — 12-R 계약 없음, 대여소 7개)
│   ├── assets/
│   │   ├── logo.svg                 로그인 화면 로고(검정+노랑)
│   │   ├── logo-navy.svg            AppHeader(홈·마이페이지·내역조회) 로고
│   │   ├── chatbot.svg / chatbot2.svg  홈 화면 챗봇 플로팅 버튼 아이콘
│   │   └── campus-map.svg           가상 캠퍼스 지도 배경(추상적 플레이스홀더)
│   ├── components/
│   │   ├── layout/
│   │   │   ├── Screen.tsx           SafeAreaView 기반 공통 화면 래퍼(`background`/`edges` prop)
│   │   │   └── AppHeader.tsx        홈·마이페이지·내역조회 공용 헤더(로고+학교명 정중앙+알림 벨,
│   │   │                            edge-to-edge 흰 배경 + 상태바 영역까지 확장). 알림 벨은
│   │   │                            `onPressAlert` prop이 있을 때만 눌림(현재 홈만 연결, 마이페이지는
│   │   │                            아직 미연결)
│   │   └── common/
│   │       ├── Button.tsx
│   │       ├── TextField.tsx        multiline·labelClassName·errorMessage 옵션 지원. secureTextEntry
│   │       │                        필드는 우측에 눈 아이콘(표시/숨김 토글)이, 값이 있는 단일행
│   │       │                        필드에는 X(지우기) 아이콘이 자동으로 붙음(멀티라인 제외)
│   │       ├── SelectField.tsx      Modal+Pressable 커스텀 드롭다운, labelClassName·errorMessage 지원
│   │       ├── ConfirmModal.tsx     공용 확인 모달(X 닫기·경고 배너·primary/danger 버튼) — 연장/신고/
│   │       │                        문의하기 제출 확인에서 재사용
│   │       ├── LoadingScreen.tsx
│   │       └── ErrorBanner.tsx
│   ├── navigation/
│   │   ├── RootNavigator.tsx        인증 상태(accessToken 유무)로 LoginScreen ↔ RootStackNavigator 분기
│   │   ├── RootStackNavigator.tsx   최상위 스택 — Tabs(하단 탭) ↔ Chatbot ↔ Notification(둘 다 탭
│   │   │                            바깥, 네이티브 헤더)
│   │   ├── TabNavigator.tsx         하단 탭 4개(홈/지도/내역조회/마이페이지) + MaterialCommunityIcons
│   │   └── MyPageStackNavigator.tsx MyPage 탭 내부 스택 — MyPageHome ↔ Inquiry
│   ├── hooks/
│   │   └── useAlertPolling.ts       로그인 상태일 때만 ALERT-001을 60초 간격으로 재조회, 새
│   │                                alertKey가 있으면 로컬 알림 배너 표시. RootNavigator.tsx에서
│   │                                accessToken 유무로 enabled 제어
│   ├── lib/
│   │   ├── secureStorage.ts         expo-secure-store 래퍼 — accessToken/expiresAt만 저장(saveSession/
│   │   │                            loadSession/clearSession)
│   │   └── notifications.ts         expo-notifications 래퍼 — 포그라운드 배너 핸들러 등록, 권한
│   │                                요청(Android는 채널 선생성 필수), 알림 1건 표시
│   ├── store/
│   │   ├── useAuthStore.ts          Zustand — accessToken/user/isHydrating 상태 + setAuth/clearAuth/
│   │   │                            hydrate(부팅 시 SecureStore에서 토큰 복원)
│   │   ├── useAlertReadStore.ts     Zustand + persist(AsyncStorage) — 알림 읽음 alertKey Set, 기기
│   │   │                            로컬에만 영속(서버 저장 없음, ALERT-002 여전히 미확정).
│   │   │                            로그아웃해도 초기화 안 함(재로그인 후에도 유지, 사용자 확인).
│   │   │                            Set은 JSON 기본 직렬화가 안 돼 replacer/reviver로 배열 변환
│   │   └── useAlertNotifiedStore.ts Zustand + persist(AsyncStorage) — "이미 로컬 배너로 알려준"
│   │                                alertKey Set(읽음 여부와는 별개 개념). hasSeeded로 첫 폴링은
│   │                                배너 없이 기준선만 세움(기존 알림 한꺼번에 쏟아짐 방지)
│   ├── types/
│   │   ├── auth.ts / home.ts / rental.ts / weather.ts / settlement.ts / inquiry.ts / user.ts
│   │   │                           각 API 응답 타입(12-R 원문 그대로. inquiry.ts·user.ts의 일부 필드는 예외 —
│   │   │                           파일 내 주석 참고)
│   │   ├── chatbot.ts               CHATBOT-001 타입(Intent 7종·Action 5종·Source 3종·오류 코드 7종,
│   │   │                            §5.13/§6 원문 값만 사용)
│   │   ├── history.ts               RENTAL-002 타입(status 필터 유니언, §5.6 원문 그대로)
│   │   ├── alert.ts                 ALERT-001 타입(type 5종·action.type 3종, §5.11 원문 값만 사용)
│   │   ├── station.ts               STATION-001 타입(잠정 구조 — 12-R 계약 없음, id/name/x/y/availableCount)
│   │   └── svg.d.ts                 *.svg import 타입 지원
│   └── screens/
│       ├── SCR-APP-AUTH-001/LoginScreen.tsx          완성(Phase 2) — 로고, 로그인/로그아웃 Mock 연동
│       ├── SCR-APP-HOME-001/HomeScreen.tsx           완성(Phase 3) — 사용자 정보·날씨 위젯·미정산·
│       │                                             챗봇 진입 버튼·알림 벨(Phase 6에서 실연결)
│       ├── SCR-APP-MYPAGE-001/MyPageScreen.tsx       완성(Phase 4) — 프로필·현재 대여·미정산·연장/신고
│       │                                             확인 모달·문의·로그아웃
│       ├── SCR-APP-INQUIRY-001/InquiryScreen.tsx     완성(Phase 4) — 문의 폼 + 제출 확인 모달
│       ├── SCR-APP-CHATBOT-001/ChatbotScreen.tsx     완성(Phase 5) — 6개 Variant, CHATBOT-001 Mock 연동,
│       │                                             OPEN_HISTORY CTA는 Phase 6에서 실연결
│       ├── SCR-APP-NOTIFICATION-001/NotificationScreen.tsx  완성(Phase 6) — 목록·읽음(실제 스와이프)·
│       │                                             CTA 라우팅. 읽음 상태는 화면 로컬이 아니라
│       │                                             useAlertReadStore(전역+영속)를 사용해 홈/
│       │                                             마이페이지/내역 헤더 배지에도 반영됨
│       ├── SCR-APP-HISTORY-001/HistoryScreen.tsx     완성(Phase 6) — 필터 탭 5개, 상태 Chip·정산 뱃지,
│       │                                             카드 펼침(CARD_EXPANDED)
│       └── SCR-APP-STATION-001/                      완성(Phase 7) — MapScreen.tsx + CampusMap.tsx +
│           StationMarker.tsx / StationBottomSheet.tsx / hooks/useCampusMapGesture.ts. 실제 지도 SDK
│           없이 SVG 가상 캠퍼스 지도(DEC-SF-004 RESOLVED)
└── docs/
    ├── PROJECT_GUIDE.md / app-screen-flow.md / PHASE.md
    └── plans/
        ├── 2026-07-31-SCR-APP-COMMON.md
        ├── 2026-07-31-SCR-APP-AUTH-001.md
        ├── 2026-08-04-SCR-APP-HOME-001.md
        ├── 2026-08-04-SCR-APP-MYPAGE-001.md
        ├── 2026-08-04-SCR-APP-MYPAGE-001-modals.md
        ├── 2026-08-05-SCR-APP-CHATBOT-001.md
        ├── 2026-08-05-SCR-APP-CHATBOT-001-ui-refresh.md
        └── 2026-08-05-SCR-APP-NOTIFICATION-001-HISTORY-001.md
```

---

## 변경 이력

- 2026-07-31: Expo(React Native + TypeScript) 프로젝트 초기화. SDK 57로 생성됐으나 스토어 배포 Expo Go가
  SDK 54까지만 지원해 `expo@^54.0.0` + `expo install --fix`로 다운그레이드.
- 2026-07-31: React Navigation(`@react-navigation/native`, `bottom-tabs`, `native-stack`) + Zustand 설치.
  스타일링은 NativeWind(Tailwind 문법)로 결정, 설정 파일 일체 구성(`tailwind.config.js`/`babel.config.js`/
  `metro.config.js`/`global.css`). 설정 직후 `babel-preset-expo`가 devDependency로 명시돼 있지 않아 별도 설치.
- 2026-07-31: PLAN(`docs/plans/2026-07-31-SCR-APP-COMMON.md`) 승인 후 Phase 1 구현 — 공통 컴포넌트
  (`Screen`/`Button`/`TextField`/`LoadingScreen`/`ErrorBanner`), 하단 탭 내비게이션, 자리표시자 화면 4개
  (홈/이용내역/마이페이지/챗봇)로 최초 구성. 색상 팔레트는 Kiosk `PROJECT_GUIDE.md` §14.1 값 재사용(사용자 확인).
- 2026-07-31(정정): 사용자가 실제 앱 와이어프레임(PDF, 16화면)을 공유 — 노션 문서 기준으로 "이번 범위 아님"
  분류했던 대여 연장·분실/파손 신고·지도·문의하기·알림 화면이 이미 와이어프레임에 존재함을 확인. 노션 문서와
  와이어프레임이 충돌하면 일괄 규칙 대신 그때그때 사용자에게 확인하기로 방침 변경. 첫 적용으로 하단 탭을
  와이어프레임 기준(홈/지도/내역조회/마이페이지)으로 교체, 챗봇은 탭에서 제외(홈 화면 플로팅 버튼으로 추후
  연결 예정), `MapScreen.tsx` 자리표시자 추가. 상세는 `docs/PROJECT_GUIDE.md` §12 참고.
- 2026-07-31: NativeWind 스타일 적용 여부를 홈 화면에 `Button`/`TextField` 임시 렌더링으로 육안 확인(골드
  버튼·테두리 입력창 정상 표시) 후 테스트 코드 제거. Phase 1 핵심 구현 완료.
- 2026-08-04: 서버 상태 관리 라이브러리로 `@tanstack/react-query` 확정 도입(사용자 승인), `axios`도 함께
  설치(아직 미사용 — Spring 실연동 시점에 연결 예정, Mock API 방식은 유지). `App.tsx`에 `QueryClientProvider`
  적용(인라인 `QueryClient` 생성). `useAuthStore.ts`는 API 호출 로직을 제거하고 상태 + `setAuth`/`clearAuth`
  setter만 갖도록 단순화, 실제 로그인/로그아웃 호출은 `LoginScreen.tsx`/`MyPageScreen.tsx`에서 각각
  `useMutation`으로 수행하도록 리팩터링. `npx tsc --noEmit` 통과 확인.
- 2026-08-04: 로고 이미지(`logo.svg`)를 홈 화면이 아니라 로그인 화면(`LoginScreen.tsx`) 상단 "빌리산" 텍스트
  자리에 적용(원래 요청이 로그인 화면용이었음). 홈 화면은 로고를 제거하고 원래 자리표시자로 복원(이후
  Phase 3에서 실제 내용으로 재작성됨).
- 2026-08-04: PLAN(`docs/plans/2026-08-04-SCR-APP-HOME-001.md`) 승인 후 Phase 3(`SCR-APP-HOME-001`) 구현.
  신규: `src/types/home.ts`(`HOME-001` 응답 타입), `src/types/rental.ts`(`RentalStatus` 상수 — §9.2·12-R에
  이미 등장하는 값만 사용), `src/api/homeApi.ts`(`HOME-001` Mock, `MOCK_SCENARIO`로 대여없음/대여중/연체+미정산
  전환). `HomeScreen.tsx`를 실제 UI로 재작성 — 사용자 정보, 현재 대여 요약(상태 Chip), 미정산 요약, 알림
  개수 배지(알림 화면 미착수라 탭 무동작), 날씨 자리표시자(`WEATHER-001` 미호출), 지도 탭 이동 버튼(지도
  자체는 렌더링 안 함), 챗봇 플로팅 버튼(no-op, 챗봇 Phase에서 연결 예정), Pull-to-Refresh. `TabNavigator.tsx`에
  `TabParamList` 타입 추가(탭 간 타입 안전 네비게이션, `any` 회피). `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 사용자가 실제 홈 화면 목업 스크린샷 공유 — `HomeScreen.tsx`를 목업 레이아웃대로 재작성.
  날씨는 애초 "준비 중" placeholder였으나 사용자 확인 후 Mock 데이터 기반 상세 위젯(시간대별 강수확률 +
  우산 추천 배너)으로 변경(`src/types/weather.ts`, `src/api/weatherApi.ts` 신규 — `WEATHER-001`(§5.12) 응답
  형태 그대로, `provider`는 문서대로 `"UNDECIDED"` 고정, 실제 외부 공급자 호출 없음). 중앙 "현재 상태·지도"
  영역(GAP-004)은 목업처럼 당장 빈 박스만 두기로 하고, 기존에 만들었던 "현재 대여 요약 카드 + 지도에서 보기
  버튼"은 제거(재배치는 이 영역 설계 확정 후). 헤더·인사말·챗봇 버튼도 목업 스타일로 조정. 상세 결정 근거는
  `docs/plans/2026-08-04-SCR-APP-HOME-001.md` §7 참고. `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 아이콘 라이브러리로 `@expo/vector-icons`(`npx expo install`로 SDK 54 호환 버전 설치,
  `react-native-vector-icons`와 동일한 아이콘 세트를 Expo Go 네이티브 링크 없이 사용 가능해 채택) 도입.
  `HomeScreen.tsx`의 이모지 아이콘(🔔☂️ⓘ 및 날씨 아이콘)을 `MaterialCommunityIcons`(`bell-outline`,
  `information-outline`, `umbrella-outline`, `weather-sunny/rainy/snowy/cloudy`)로 교체. 아이콘 이름은
  `keyof typeof MaterialCommunityIcons.glyphMap`으로 타입 지정(`any` 회피). `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 날씨 아이콘 선택 기준을 `condition`이 아니라 강수확률 구간으로 변경(사용자 확인) —
  0~29% `weather-cloudy`, 30~59% `weather-partly-rainy`, 60% 이상 `weather-rainy`. `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 하단 탭(`TabNavigator.tsx`)에 `MaterialCommunityIcons` 1차 적용 — 홈/지도/내역조회는
  포커스 여부로 채워진 아이콘↔윤곽 아이콘을 전환하고, 마이페이지는 `account-circle`/`account-circle-outline`
  사용.
- 2026-08-04(추가, 실제 탭 바 목업 스크린샷 확인 후 수정): 위 1차 아이콘 선택을 목업에 맞춰 교체 — 아이콘
  자체는 포커스와 무관하게 고정하고(홈 `home-outline`, 지도 `map-outline`, 내역조회 `history`, 마이페이지
  `account`— 마이페이지만 목업처럼 채워진 사람 아이콘), 활성 탭 표시는 아이콘 교체 대신 배경색으로만
  구분(`tabBarActiveBackgroundColor: '#FFEDC0'`(secondary 토큰) + `tabBarItemStyle`로 각 탭 아이템에 둥근
  모서리·여백 적용). `tabBarActiveTintColor`는 대비를 위해 어두운 텍스트 색(`#1E2A38`)으로 변경, 비활성은
  회색 토큰(`#5F5E5E`) 유지. 목업의 "탭 바 전체가 떠 있는 알약 모양" 스타일(둥근 외곽·마진)은 이번엔 반영하지
  않음 — SafeArea·컨텐츠 패딩 재조정이 필요한 별도 작업이라 범위에서 제외, 필요하면 후속으로 진행.
  `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 사용자 지시로 `PHASE.md`의 Phase 순서를 재정렬 — 기존 "Phase 8(범위 미정)"에 묶여
  있던 지도·대여소 상세·알림·문의를 다시 나눔. 알림(`SCR-APP-NOTIFICATION-001`)을 챗봇보다 먼저(새
  Phase 4)로 앞당기고 챗봇은 Phase 5로 밀림, 문의(`SCR-APP-INQUIRY-001`)는 마이페이지 조회 Phase(Phase 6)에
  묶고, 지도(`SCR-APP-STATION-001`)와 대여소 상세(`SCR-APP-STATION-DETAIL-001`)는 서로 묶어 맨 마지막
  Phase 9로 미룸. `DEC-SF-004`(지도 SDK)·`DEC-SF-008`(알림 영속성)·`DEC-SF-009`(문의 저장)가 미확정이라는
  사실 자체는 그대로 — 착수 순서만 재배치. 상세는 `docs/PHASE.md` 상단 메모 참고. 아직 코드 변경은 없음.
- 2026-08-04(추가, 재정렬 수정): 사용자 지시로 위 순서 중 마이페이지(조회)+문의와 알림을 다시 맞바꿈 —
  마이페이지(조회 전용) + 문의가 Phase 4(챗봇보다 먼저, 현재 CURRENT PHASE)로, 알림은 Phase 6(챗봇 다음)
  으로 이동. 챗봇(Phase 5)·결제(Phase 7)·이용내역(Phase 8)·지도+대여소상세(Phase 9) 순서는 그대로.
  아직 코드 변경은 없음(다음 착수 대상은 Phase 4 `SCR-APP-MYPAGE-001`(조회) + `SCR-APP-INQUIRY-001`).
- 2026-08-04: PLAN(`docs/plans/2026-08-04-SCR-APP-MYPAGE-001.md`) 승인 후 Phase 4 구현. 신규 Mock API
  4개(`userApi.ts`/`rentalApi.ts`/`settlementApi.ts`/`inquiryApi.ts`) + 타입 3개(`settlement.ts`/
  `inquiry.ts`/`user.ts`) 추가, `rental.ts`에 `CurrentRentalDetail`(RENTAL-001 응답) 추가. 사용자가 실제
  마이페이지 목업 스크린샷을 공유해 구현 중 두 가지를 확인·반영: (1) `USER-001` 공식 응답엔 없는 학번
  필드를 목업처럼 Mock에 한해 추가(`types/user.ts` 주석으로 비공식 필드임을 명시), (2) `DEFERRED_NOT_CONTRACTED`
  상태인 연장하기·신고하기 버튼을 자리만 배치하고 `onPress`는 no-op으로(Home 챗봇 버튼과 동일 패턴).
  `MyPageScreen.tsx`를 프로필(아바타 이니셜+이름+학번)·현재 대여 카드(상태 Chip·진행바·남은 시간 또는 연체
  정보)·미정산 카드(있을 때만, 결제 버튼은 Phase 7 연결 예정 no-op)·문의하기/로그아웃 리스트로 재작성.
  문의하기(`SCR-APP-INQUIRY-001`)는 `DEC-SF-009` 미확정 상태에서 사용자가 네이티브 폼 방식으로 결정 —
  `InquiryScreen.tsx` 신규(유형/제목/내용 입력, 클라이언트 필수값 검증, Mock 제출) — `INQUIRY-001` 자체가
  12-R 미확정이라 Request/Response 구조는 잠정 설계임을 `types/inquiry.ts`·`inquiryApi.ts` 주석에 명시.
  `MyPageStackNavigator.tsx` 신규 — MyPage 탭 내부에서만 `MyPageHome ↔ Inquiry` push/back(다른 탭과 달리
  이 스택만 네이티브 헤더 사용, MyPageHome은 Home과 통일감 있게 자체 헤더를 그리도록 `headerShown: false`
  개별 설정). `TextField.tsx`에 `multiline`/`numberOfLines` 옵션 추가(기존 사용처는 영향 없음).
  `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 문의 화면(`SCR-APP-INQUIRY-001`) 입력 UI 보완. "유형"을 자유 텍스트에서 드롭다운으로
  변경 — 옵션 7종(대여/반납 문의, 반납기한 연장, 분실/파손 신고, 결제/정산 문의, 대여소/시설 문의,
  계정/로그인 문의, 기타)을 `types/inquiry.ts`의 `INQUIRY_TYPES` 상수로 중앙 관리. "반납기한 연장"·
  "분실/파손 신고"는 실제 연장/신고 기능(`DEFERRED_NOT_CONTRACTED`)이 아니라 문의 카테고리 라벨일 뿐.
  드롭다운은 새 라이브러리(`@react-native-picker/picker` 등) 없이 `Modal`+`Pressable` 기반 커스텀
  `SelectField.tsx` 컴포넌트로 구현(사용자가 라이브러리 미설치 방식으로 명시적으로 선택, 2026-08-04).
  "내용" 입력은 `TextField.tsx`에 `multiline` 시 `min-h-[220px]` 적용해 textarea처럼 넓게 조정,
  `numberOfLines`도 6→12로 확대. `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): `MyPageStackNavigator.tsx`의 `Inquiry` 화면 옵션에 `headerBackButtonDisplayMode: 'minimal'`
  추가 — 뒤로가기 버튼에 이전 화면 라우트 이름("MyPageHome")이 텍스트로 노출되던 것을 화살표만 남도록 수정.
- 2026-08-04(추가): PLAN(`docs/plans/2026-08-04-SCR-APP-MYPAGE-001-modals.md`) 승인 후 마이페이지
  연장하기·신고하기 확인 모달 구현. 사용자가 공유한 실제 와이어프레임(Extend modal/Frame 13)을 참고.
  `DEC-SF-006`(연장)·`DEC-SF-007`(신고)는 여전히 `DEFERRED_NOT_CONTRACTED`라 확인 버튼을 눌러도 서버 반영은
  없고 모달만 닫히는 Mock으로 구현(사용자 확인, "UI 모달만"). 신규 `src/components/common/ConfirmModal.tsx`
  (X 닫기·제목/설명·선택적 경고 배너·취소/확인 버튼, `confirmVariant`로 primary/danger 색 전환 — 두 모달
  구조가 동일해 공용 컴포넌트로 작성). `MyPageScreen.tsx`의 기존 no-op 연장/신고 버튼에 모달 상태 연결.
  새 API·타입은 추가하지 않음(계약 자체가 없음). `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): `ConfirmModal.tsx` 스타일 조정 — 취소 버튼 텍스트를 `text-text-tertiary`(갈색)에서
  `text-text`(검정)로 변경, 카드 안쪽 여백을 `p-6` → `px-8 py-16`까지 단계적으로 확대(사용자 확인 반복).
- 2026-08-04(추가): 사용자 지시로 화면 배경 규칙 확정 — 홈/지도/내역조회/마이페이지/로그인 5개 화면만
  `bg-background`(연회색)를 쓰고, 그 외 화면은 흰 배경이어야 함. `src/components/layout/Screen.tsx`에
  `background?: 'default' | 'white'` prop 추가(기본값 `'default'`, 기존 화면들은 변경 없음).
  `InquiryScreen.tsx`(2곳)·`ChatbotScreen.tsx`(자리표시자)에 `background="white"` 적용. 앞으로 새로
  만드는 화면도 이 5개 목록에 없으면 `background="white"`를 써야 함. `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 마이페이지 연체중 상태 렌더링을 실제 목업 스크린샷 기준으로 재작성 — 기존 "연체
  N일째 · 연체료 금액" 텍스트를 "반납 기한 / 기한 지남" 라벨 행 + 꽉 찬 빨간 진행바로 교체(정상 대여중
  레이아웃은 유지). 연체 금액 자체는 별도 재계산·표시하지 않고 아래 미정산 카드의 서버 산정값으로만
  노출(§7.1 원칙). 미정산 카드도 세로 나열 → 좌측 "미정산 금액"+금액, 우측 빨간 알약 버튼("결제하기")
  가로 배치로 재작성, 배경을 `bg-secondary`(황토색)에서 `bg-error-bg`(연핑크)로 변경. 확인을 위해
  `rentalApi.ts`/`settlementApi.ts`의 `MOCK_SCENARIO`를 임시로 `OVERDUE_UNSETTLED`로 전환(원복 여부는
  사용자 선택 대기 중). `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 홈·마이페이지 헤더(로고+학교명+알림 아이콘 행) 구분 방식을 사용자 지시로 수정 —
  처음엔 `border-b` 구분선으로 넣었으나 바로 번복, 다른 카드들과 같은 `rounded-2xl bg-white p-4` 스타일로
  헤더 자체를 흰 배경 카드로 변경(전체 배경은 `bg-background` 연회색 유지, 헤더만 흰색으로 도드라짐).
  지도·내역조회는 아직 자리표시자라 헤더 행 자체가 없어 이번엔 적용하지 않음 — 각 화면 실제 구현
  (Phase 9/Phase 8) 때 동일하게 반영 예정. `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 마이페이지 "현재 대여 현황" 카드의 상태 칩 로직 보완 — 활성 대여가 없어도(`rental`이
  `null`) 미정산이 남아있으면(`settlements.items.length > 0`) 빈칸 대신 "미정산" 칩(`bg-error-bg`/
  `text-error-text`)을 표시. 대여가 있으면 기존 연체중/대여중 칩 우선. `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 홈·마이페이지 헤더가 화면 가장자리까지 닿지 않던 문제 수정 — 헤더가 `ScrollView`의
  `contentContainerClassName="px-6 ..."` 안에 있어서 `w-full`을 줘도 이미 좌우 24px 파인 영역 기준
  100%였을 뿐이었음. `-mx-6`으로 부모 padding을 상쇄하고 `px-6`을 헤더 자체에 다시 줘서 흰 배경만
  화면 끝까지 닿게(edge-to-edge) 수정, `rounded-2xl`은 제거(가장자리에 닿으니 불필요). `npx tsc --noEmit`
  통과 확인.
- 2026-08-04(추가): 헤더(로고+학교명+알림 벨)가 `HomeScreen.tsx`/`MyPageScreen.tsx`에 중복 작성되어 있어
  스타일 수정할 때마다 두 곳을 따로 고쳐야 했고 실제로 알림 배지 점 유무가 어긋나 있었음(홈만 있었음).
  사용자 지시로 `src/components/layout/AppHeader.tsx` 공용 컴포넌트로 추출(`schoolName`/`alertCount`
  props), 두 화면 다 이걸 사용하도록 교체 — 마이페이지에도 동일한 알림 배지를 적용(사용자 확인,
  "마이페이지도 알림배지야"). 마이페이지는 원래 `USER-001`/`RENTAL-001`/`SETTLEMENT-001`만 쓰는데
  (app-screen-flow.md §2.5 매핑엔 `HOME-001`이 없음) 배지 숫자만 필요해서 새 API·타입을 만드는 대신
  기존 `HOME-001`(`getHomeSummary`)의 `alertCount` 필드를 재사용 — 문서 매핑과 정확히 일치하진 않는
  실용적 선택이라는 점을 코드 주석에도 남김. 전용 알림 카운트가 필요해지면(`ALERT-001` 기반) Phase 6
  (알림) 때 다시 정리 예정. `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 마이페이지 "미정산" 칩 색상을 연체중 칩과 같은 빨강 계열(`bg-error-bg`/`text-error-text`)
  에서 보라 계열(`bg-purple-100`/`text-purple-700`, Tailwind 기본 팔레트)로 변경 — 연체와 시각적으로
  구분되도록(사용자 확인). `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): `AppHeader`와 세이프 에어리어 상단 사이에 뜨던 여백 수정 — 헤더가 `ScrollView`의
  `contentContainerClassName="...pt-6"` 안 첫 번째 자식이라 그 `pt-6`이 헤더보다 먼저 적용되고 있었음.
  `AppHeader`를 `ScrollView` 밖, `Screen` 바로 아래로 이동(스크롤과 무관하게 항상 화면 맨 위 고정) —
  `HomeScreen.tsx`/`MyPageScreen.tsx` 둘 다 적용. `AppHeader.tsx`의 `-mx-6`은 더 이상 상쇄할 부모 padding이
  없어져서 제거(그대로 두면 화면 밖으로 24px씩 밀려남). `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 상태바(노치) 영역까지 헤더와 같은 흰 배경이 이어지도록 수정 — 기존엔 `Screen.tsx`의
  `SafeAreaView`가 top 세이프 에어리어까지 자기 배경색(`bg-background` 연회색)으로 칠해서, `AppHeader`
  흰 배경이 상태바 아래에서부터만 보였음. `Screen.tsx`에 `edges` prop 추가(기본 전체 4방향, 필요시
  일부만 지정), `HomeScreen.tsx`/`MyPageScreen.tsx`는 `edges={['right','bottom','left']}`로 top을 빼서
  `Screen`이 상단을 비워두게 함. 대신 `AppHeader.tsx`가 `useSafeAreaInsets()`로 `insets.top`만큼
  `paddingTop`을 직접 줘서 상태바 영역까지 흰 배경이 이어짐(edge-to-edge, 상단 포함). 헤더 없는 다른
  화면(에러 상태 포함)은 `Screen` 기본값 그대로라 영향 없음. `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 문의하기(`SCR-APP-INQUIRY-001`) 제출 흐름을 사용자 지시로 재구성 — 기존엔 제출 후
  전체 화면 성공 문구("문의가 접수됐어요")를 보여줬는데, 이를 제출 **전** 확인 모달로 교체. "문의하기"
  버튼은 이제(유효성 검증 통과 시) `ConfirmModal`을 열고, 모달 제목 "접수하시겠습니까?"/설명 "확인 후
  순차적으로 답변드리겠습니다"/버튼 취소·접수하기(`confirmVariant="primary"`)로 구성. 모달의 "접수하기"를
  누르면 그때 실제 `submitInquiry` Mock을 호출 — 성공 시 모달을 닫고 마이페이지로 `goBack()`, 실패 시
  모달만 닫고 기존 `ErrorBanner`가 보이게 함(에러 상태에서도 모달이 그대로면 아래 배너가 가려짐). 연속
  탭으로 중복 제출되는 걸 막기 위해 `onConfirm`에서 `inquiryMutation.isPending`일 때는 `mutate` 호출을
  건너뛰도록 가드 추가. 이제 쓰지 않는 `submitted` 상태·전체화면 성공 뷰는 제거. `npx tsc --noEmit` 통과
  확인.
- 2026-08-04(추가): `TextField.tsx`/`SelectField.tsx`에 `labelClassName`(기본값은 기존과 동일한
  `text-sm font-medium text-text-secondary`)·`errorMessage` prop 추가 — 다른 사용처(로그인 화면 등)는
  기본값 그대로라 영향 없음. `InquiryScreen.tsx`에서만 `labelClassName`으로 라벨(유형/제목/내용) 크기를
  키우고, 필드별 `errorMessage`(유형/제목/내용을 각각 입력해주세요)를 각 입력 바로 아래 표시하도록 변경 —
  기존에 폼 하단에 한 줄로 몰아서 보여주던 공용 에러 문구("유형·제목·내용을 모두 입력해주세요")는 제거.
  `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): `AppHeader.tsx`의 학교명 텍스트가 로고·벨 아이콘 너비 차이 때문에 화면 정중앙이 아니라
  "로고와 벨 사이 남은 공간"의 중앙에 표시되던 문제 수정 — 로고와 벨을 각각 `flex-1`(좌측 `items-start`,
  우측 `items-end`) 래퍼로 감싸 양쪽이 항상 동일한 너비를 갖게 하고, 그 사이 텍스트를 `text-center`로 배치
  (좌우 패딩이 대칭이라 화면 진짜 중앙과 일치). `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): 사용자 승인 후 Prettier 도입 — `prettier`·`prettier-plugin-tailwindcss`를 devDependency로
  설치, `.prettierrc.json`(세미콜론 없음·홑따옴표·`trailingComma: all`·`printWidth: 100`, tailwind 플러그인
  연결)·`.prettierignore` 추가. `package.json`에 `format`/`format:check` 스크립트 추가. VSCode에서 저장 시
  자동 포맷(및 className 자동 정렬)이 실제로 동작하도록 `.vscode/settings.json`도 추가(`esbenp.prettier-vscode`
  확장 설치 필요, 워크스페이스에 없으면 사용자가 설치해야 적용됨). 기존 코드 전체 재포맷(`npm run format`)은
  이번엔 실행하지 않음 — 무관한 파일까지 대량으로 바뀌는 diff가 생기므로, 필요하면 별도로 요청받아 진행.
- 2026-08-04(추가): 마이페이지 현재 대여 카드에서 대여소명(`checkoutStation.name`)과 대여 일시
  (`formatRentedAt(rentedAt)`)를 세로로 나열하던 걸 한 줄 `flex-row justify-between`으로 변경(사용자 확인).
  `npx tsc --noEmit` 통과 확인.
- 2026-08-04(추가): `ConfirmModal.tsx`의 X 닫기 버튼을 카드 padding(`p-8`)과 무관하게 항상 카드 상단 가까이
  붙도록 수정 — 기존엔 X가 카드 내부 첫 번째 flow 요소라 padding이 커질수록(`py-16`까지 늘렸던 이력 있음)
  같이 밀려 내려갔음. 카드(`Pressable`)에 `relative` 추가하고 X 버튼을 `absolute right-4 top-4`로 빼서
  padding 값과 무관하게 고정 위치를 갖게 함, 제목/설명 블록은 그대로 padding을 따라 흐름. `npx tsc --noEmit`
  통과 확인.
- 2026-08-04(추가): 이번 세션의 마이페이지 UI 다듬기(연장·신고 확인 모달, 문의하기 제출 흐름 변경, 공용
  헤더 `AppHeader.tsx` 분리, Prettier 도입 등)를 `docs/PHASE.md` Phase 4 항목에도 "2026-08-04(추가)" 메모로
  반영 — 문서와 코드 상태 일치. 새 API·타입 추가는 없어 Phase 4 실질 범위(§ 문서 §5 계약)는 그대로.
  커밋 히스토리도 정리(브랜치 `S15P11A201-133-fe-mypage`, 원격 push 이력 없어 로컬 전용): `chore: Prettier
  도입`·`style: 헤더, 마이페이지, 모달 UI 조정`·`feat: 문의하기 유형 추가 및 UI 수정` 3개 커밋에
  `S15P11A201-133` prefix 통일, 메시지가 잘못 붙었던 커밋 1개는 내용이 같은 커밋에 squash. 다음 착수
  대상은 Phase 5(`SCR-APP-CHATBOT-001`, 챗봇).
- 2026-08-05: PLAN(`docs/plans/2026-08-05-SCR-APP-CHATBOT-001.md`) 승인 후 Phase 5(`SCR-APP-CHATBOT-001`)
  구현. 홈 화면 챗봇 플로팅 버튼이 탭 바깥 화면으로 나갈 방법이 없어서 최상위에 `RootStackNavigator.tsx`
  (신규, `Tabs ↔ Chatbot`)를 추가하고 `RootNavigator.tsx`가 `TabNavigator` 대신 이걸 렌더링하도록 수정.
  `TabNavigator.tsx`의 `TabParamList.MyPage`를 `NavigatorScreenParams<MyPageStackParamList>`로 바꿔
  챗봇에서 `Tabs → MyPage → Inquiry`까지 중첩 네비게이션이 타입 안전하게 되도록 함. `Chatbot` 화면은
  `Inquiry`와 동일하게 네이티브 기본 헤더 + `headerBackButtonDisplayMode: 'minimal'` 뒤로가기 사용(사용자
  확인, 모달 프레젠테이션 대신 기본 push 전환으로 결정).
  신규 `src/types/chatbot.ts`(Intent 7종·Action 5종·Source 3종·오류 코드 7종, PROJECT_GUIDE.md §5.13/§6
  원문 값만 사용) + `src/api/chatbotApi.ts`(`CHATBOT-001` Mock, `MOCK_SCENARIO`로 SUCCESS/FALLBACK/
  TIMEOUT/ERROR 전환, 질문 키워드로 P0 Intent 6종 매칭하는 규칙 기반 STATIC_FAQ 응답).
  `ChatbotScreen.tsx` 전체 재작성 — `INITIAL`(P0 Intent 1:1 대응 추천 질문 6개 칩), `ANSWERING`(전송 즉시
  사용자 메시지 표시 + "답변 준비 중"), `ANSWERED`(답변 버블 + 허용 CTA 버튼), `FALLBACK_FAQ`(절제된 보조
  문구만 추가 표시, 기술적 노출 없음), `TIMEOUT`/`ERROR`(입력 보존 + 다시 시도·문의하기 버튼) 전부 구현.
  CTA는 `OPEN_MY_PAGE`·`OPEN_INQUIRY`만 실제 라우팅 연결(Phase 4 완료로 대상 화면 있음), `OPEN_HISTORY`
  (Phase 8)·`OPEN_MAP`(Phase 9)은 PHASE.md 명시대로 버튼만 두고 라우팅 없음. 대화 이력은 컴포넌트 로컬
  state에만 유지(영속 저장 없음, 화면 벗어나면 소실). `CHATBOT_MESSAGE_TOO_LONG`·`CHATBOT_UNSAFE_INPUT`·
  `CHATBOT_RATE_LIMITED` 오류는 타입만 정의하고 Mock 시나리오로 구현하지 않음(요청 범위 밖). `npx tsc
  --noEmit` 통과 확인. `docs/PHASE.md` Phase 5 상태를 DONE으로 갱신, 다음 착수 대상은 Phase 6
  (`SCR-APP-NOTIFICATION-001`, 알림).
- 2026-08-05(추가): 홈 화면 챗봇 플로팅 버튼을 텍스트("챗봇") 알약 버튼에서 `src/assets/chatbot.svg`
  아이콘(64x64)으로 교체 — SVG 자체가 이미 둥근 사각형 배경을 포함하고 있어 기존 `bg-secondary` 배경·
  패딩 래퍼는 제거. `npx tsc --noEmit` 통과 확인. (참고: 이 SVG는 base64 PNG를 품은 파일이라 용량이
  약 1.8MB로 큼 — 번들 크기에 영향 있을 수 있어 추후 최적화 여지로 남겨둠.)
- 2026-08-05(추가): 사용자가 챗봇 화면 목업 스크린샷 공유 → PLAN
  (`docs/plans/2026-08-05-SCR-APP-CHATBOT-001-ui-refresh.md`) 승인 후 `SCR-APP-CHATBOT-001` UI 재구성
  (Phase 5는 이미 DONE이라 이번은 API 계약·CTA 라우팅·오류 처리 로직 변경 없는 순수 표현 변경).
  `RootStackNavigator.tsx`의 `Chatbot` 헤더를 `headerTitle` 커스텀 컴포넌트로 교체 — "빌리" +
  "AI 어시스턴트 · 온라인"(초록 점, 팔레트에 온라인색이 없어 Tailwind 기본 `green-500` 사용, 사용자 확인)
  2줄 표시, 뒤로가기 버튼 자체는 기존 `headerBackButtonDisplayMode: 'minimal'` 그대로. `ChatbotScreen.tsx`는
  ① 화면 진입 시각을 "오늘, 오전 10:42" 형식으로 표시하는 시간 구분선(클라이언트 로컬 시각 — `CHATBOT-001`
  응답에 타임스탬프 필드가 없어 유일한 선택지, 사용자 확인) 추가, ② `INITIAL`에 화면 로컬 상수인 인사
  말풍선("안녕하세요, 빌리예요! ...", 목업 문구 그대로, 사용자 확인) 추가, ③ 추천 질문을 알약 그리드에서
  흰 카드 안 세로 리스트(행 사이 구분선)로 재구성, ④ assistant 말풍선 배경을 `bg-secondary`(크림)에서
  `bg-background` + 테두리로 변경(목업의 연회색 톤에 맞춤, 흰 배경 CTA 버튼과 더 잘 구분되도록), ⑤ CTA
  버튼에 화살표(→) 텍스트 추가(전송·로그되는 `action.label` 문자열 자체는 변경 없음). `npx tsc --noEmit`
  통과 확인. 이 환경엔 `react-native-web`/`react-dom`이 설치돼 있지 않고(웹 프리뷰 불가, 새 라이브러리라
  설치 전 승인 필요) 에뮬레이터도 없어 실제 화면 렌더링은 육안으로 확인하지 못함 — 사용자가 Expo Go로
  직접 확인 필요.
- 2026-08-05(추가): 사용자 피드백 3건 반영. ① 추천 질문을 탭해 대화가 시작돼도 인사 말풍선·추천 질문
  카드가 사라지지 않도록 `messages.length === 0` 조건 제거(항상 렌더링, 이후 메시지는 그 아래 계속 쌓임).
  ② user/assistant 채팅 말풍선의 `border border-disabled` 제거(배경색만으로 구분, 추천 질문 카드·CTA
  버튼의 테두리는 유지 — "채팅 말풍선"이 아니라서 그대로 둠). ③ 말풍선 모서리를 말풍선 꼬리 형태로
  변경 — assistant(챗봇이 보내는 말풍선)는 `rounded-tl-none`(왼쪽 상단 각짐), user(유저가 보내는
  말풍선)는 `rounded-tr-none`(오른쪽 상단 각짐), 인사 말풍선도 assistant와 동일 스타일 적용.
  `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): 채팅 텍스트(인사 말풍선·추천 질문·메시지 본문) 폰트 크기를 `text-sm` → `text-base`로
  키움(로딩·오류 안내 문구는 상태 텍스트라 제외). 전송 버튼 아이콘 색상은 사용자 확인 결과 기존 구성
  (원형 배경 `bg-primary` + 진한 아이콘)을 그대로 유지하기로 해 변경 없음. `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): 헤더 "빌리" 타이틀(`RootStackNavigator.tsx`)을 `text-base` → `text-lg`로, 채팅 텍스트
  (인사 말풍선·추천 질문·메시지 본문)를 `text-base` → `text-lg`로 한 단계 더 키움(입력창 텍스트는 대상
  아님). `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): 전송 버튼 원형 배경을 입력 여부와 무관하게 항상 `bg-primary`로 렌더(기존엔 입력이
  비어있을 때 `bg-disabled` 회색으로 바뀌었음) — 비활성 상태는 색 전환 대신 `opacity-40`으로 표시, 아이콘
  자체는 기존처럼 진한 색 유지. `ChatbotScreen.tsx`의 `KeyboardAvoidingView` `behavior`를 Android에서
  `undefined`(동작 없음) → `'height'`로 변경 — 입력창을 탭해 키보드가 올라올 때 입력창이 가려지지 않고
  키보드 위로 따라 올라오도록 수정(iOS는 기존 `'padding'` 유지). `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): 위 `KeyboardAvoidingView` `behavior: 'height'` 수정으로도 안 된다는 사용자 확인 →
  edge-to-edge Android에서 RN 코어 `Keyboard` 이벤트가 불안정한 게 흔한 원인이라, `KeyboardAvoidingView`를
  걷어내고 `react-native-reanimated`의 `useAnimatedKeyboard`(네이티브 키보드 인셋을 직접 추적, 이미
  `package.json`에 있던 의존성을 이 화면에서 처음 사용 — 새 라이브러리 추가 아님)로 교체. 키보드 높이만큼
  `Animated.View`(구 `KeyboardAvoidingView` 자리)에 `paddingBottom`을 애니메이션으로 적용해 메시지
  영역이 줄어들고 입력창이 키보드 위로 따라 올라오게 함. `isStatusBarTranslucentAndroid: true` 옵션 적용
  (프로젝트가 edge-to-edge라 상태바 투명 처리를 감안해야 정확한 키보드 높이 계산됨 — `AppHeader.tsx`가
  `insets.top`을 직접 더해 쓰는 것과 같은 맥락). `npx tsc --noEmit` 통과 확인. 에뮬레이터가 없어 실기기/
  Expo Go에서 실제 동작 확인은 사용자가 직접 해야 함.
- 2026-08-05(추가): 위 `useAnimatedKeyboard` 적용 후 실기기에서 "Runtime not ready" 오류 발생 — 원인
  파악 결과 `react-native-reanimated`의 peer dependency인 `react-native-worklets`가 `package.json`에
  명시돼 있지 않아 npm이 최신 호환 버전(`0.8.3`)을 임의로 설치했는데, Expo Go(SDK 54)는 네이티브
  바이너리에 `react-native-worklets ~0.5.1`을 고정으로 내장하고 있어 JS/네이티브 버전 불일치로 크래시가
  난 것으로 확인(`node_modules/expo/bundledNativeModules.json` 대조, `npx expo install --check`는
  `package.json`에 없는 패키지라 못 잡아냄). 사용자 승인 후 `npx expo install react-native-worklets` 실행 —
  `package.json`에 `react-native-worklets: "0.5.1"`(SDK 54 호환 버전) 명시적으로 고정 설치(26개 패키지
  제거·1개 추가, 기존 0.8.3 관련 불필요 의존성 정리됨). `npx tsc --noEmit`·`npx expo install --check`
  둘 다 통과 확인. 코드 변경 없음(패키지 버전 고정만).
- 2026-08-05(추가): 사용자 지시로 Phase 재정렬. 알림(`SCR-APP-NOTIFICATION-001`)과 이용 내역 조회
  (`SCR-APP-HISTORY-001`, 기존 Phase 8)를 하나의 Phase(새 Phase 6)로 합침 — 토스 결제 연동(기존
  Phase 7)은 순서 그대로 유지, 지도+대여소 상세(기존 Phase 9)는 빈 자리로 당겨져 Phase 8이 됨.
  `docs/PHASE.md` 갱신(재정렬 이력 노트 추가, Phase 6 구현 범위에 두 화면 모두 명시, Phase 5의
  `OPEN_HISTORY`/`OPEN_MAP` 대상 Phase 번호 주석 수정). 문서만 변경, 코드 변경 없음.
- 2026-08-05(추가): PLAN(`docs/plans/2026-08-05-SCR-APP-NOTIFICATION-001-HISTORY-001.md`) 승인 후
  Phase 6 구현. 사용자 확인 사항 3가지 반영 — ① 알림 읽음 처리는 진짜 스와이프 제스처로 구현(탭 방식
  대신), ② 이용 내역 필터 탭에 `LOST`·`FAILED`도 별도로 추가(전체/대여중/완료 3개가 아니라 5개), ③
  이용 내역 헤더는 Home/MyPage와 동일한 공용 `AppHeader` 재사용.
  신규 의존성 `react-native-gesture-handler`(Expo SDK 54 호환 `~2.28.0`, `npx expo install`로 설치) —
  알림 스와이프 읽음 처리(`Swipeable`)에 필요. RN 공식 요구사항대로 `App.tsx` 최상단을
  `GestureHandlerRootView`로 감쌈.
  신규 `src/types/history.ts`/`src/api/historyApi.ts`(`RENTAL-002` Mock, `MOCK_ITEMS` 5건으로 대여중·
  완료+정산완료·완료+미정산·분실·실패 조합 커버, `nextCursor`는 항상 `null` — 페이지네이션 UI는 이번
  Phase 범위 아님). `HistoryScreen.tsx` 전체 재작성 — 필터 탭 5개(전체/대여중/완료/분실/실패), 카드
  탭하면 로컬 state로 펼쳐서(`CARD_EXPANDED`) 반납 대여소·정확한 기한·실패 사유 표시, 상태 Chip은
  §9.2 규칙 재사용(새 Enum 없이 status+dueAt 비교로 연체중 판정), 정산 뱃지(미정산/정산완료), 헤더의
  `alertCount`/`schoolName`은 마이페이지와 동일하게 `HOME-001` 재사용.
  신규 `src/types/alert.ts`/`src/api/alertApi.ts`(`ALERT-001` Mock, type 5종 각 1건 — `read`는 응답에
  없고 화면 로컬 `Set<string>`으로만 관리, 새로고침 시 초기화되는 게 정상 동작). `NotificationScreen.tsx`
  신규 작성 — `RootStackNavigator`에 `Chatbot`과 동일한 방식(네이티브 기본 헤더 + minimal 뒤로가기)으로
  등록, 각 항목을 `Swipeable`로 감싸 오른쪽으로 스와이프하면 "읽음" 액션 노출, 항목 탭 시에도 읽음
  처리 + `action.type`이 `OPEN_MY_PAGE`/`OPEN_HISTORY`면 실제 이동.
  `AppHeader.tsx`에 `onPressAlert?: () => void` prop 추가(기존엔 벨을 눌러도 무동작) — 알림 벨을
  `Pressable`로 감싸고 prop 없으면 비활성. 홈 화면(`HomeScreen.tsx`)만 이번에 `Notification` 이동으로
  연결(마이페이지 알림 벨은 PHASE.md 명시대로 이번 Phase 범위 아니라 그대로 무동작 유지).
  `ChatbotScreen.tsx`의 `handleAction`에서 `OPEN_HISTORY` 케이스를 `Tabs → History` 실제 이동으로
  연결(기존엔 자리만 두고 no-op). `npx tsc --noEmit` 통과 확인. `docs/PHASE.md` Phase 6 상태를
  DONE으로 갱신, 다음 착수 대상은 Phase 7(Toss 결제 연동). 에뮬레이터가 없어 스와이프 제스처 등 실제
  터치 동작은 사용자가 Expo Go에서 직접 확인 필요.
- 2026-08-05(추가): 이용 내역을 대여 시작일(`rentedAt`) 기준 "YYYY년 M월"로 묶어 보여주도록 변경 —
  카드 렌더링을 `HistoryItemCard.tsx`(신규)로, 월 구분 섹션을 `HistoryMonthGroup.tsx`(신규)로 분리하고
  `HistoryScreen.tsx`는 `groupByMonth`로 묶은 그룹만 렌더링. 사용자가 공유한 목업 스크린샷 기준으로
  상태 Chip 색상도 조정 — 반납완료(회색 → `bg-green-100`/`text-green-700`), 분실·파손(주황 계열
  `error-bg` → `bg-rose-100`/`text-rose-800`), 취소됨(`bg-disabled` → `bg-background`, 더 옅은 회색).
  대여중 Chip은 기존 `rented-bg`/`rented-text`가 이미 목업과 일치해 그대로 둠. `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): `TabNavigator.tsx`의 활성 탭 필(pill)에 테두리를 추가하고 높이를 키움 —
  `tabBarActiveBackgroundColor`만으로는 border를 줄 수 없어 커스텀 `tabBarButton`(`TabBarButton`)으로
  교체. React Navigation이 커스텀 버튼엔 `focused`를 `accessibilityState`가 아니라 `'aria-selected'`
  prop으로 넘겨서 그걸로 포커스 여부 판정, 포커스 시 `borderWidth: 1.5`·`borderColor: '#FFD700'`(primary)
  적용 + `height: 52`로 높임(기존 대비 여유 있게). 예전에 남아있던 무동작 주석
  (`// tabBarActiveStyle: { borderRadius: 16 }`, 유효하지 않은 옵션이라 죽어있던 코드)도 이번에 실제
  동작하는 코드로 대체돼 정리됨. `npx tsc --noEmit` 통과 확인. 테두리 색·두께·높이 값은 임의로 고른
  값이라 실제 화면에서 보고 조정이 필요할 수 있음.
- 2026-08-05(추가): `HistoryItemCard.tsx`의 대여 장소 표시를 `checkoutStationName`만 보여주던 것에서
  "대여장소 → 반납장소"(`item.checkoutStationName} → {item.returnStationName ?? '반납 전'}`) 형태로
  변경 — `RENTAL-002`가 station ID 없이 이름 문자열만 주는 계약 그대로, 아직 반납 전이면 "반납 전"
  표시. 펼침(`CARD_EXPANDED`) 영역에 중복으로 있던 "반납 대여소: ..." 줄은 이제 카드 상단에서 바로
  보이니 제거. 긴 지명 조합이 칩을 밀어내지 않도록 상단 행에 `gap-2` + 텍스트 `flex-1` 추가.
  `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): 위 "반납 전" 표시도 사용자 지시로 다시 변경 — 반납 전일 땐 화살표(`→`)와 "반납 전"
  문구 자체를 없애고 대여장소만 보이도록(`{checkoutStationName}{returnStationName ? ` → ${returnStationName}` : ''}`).
  `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): 사용자 확인으로 `DEC-SF-005`(대여 기한·연체 기준) 해소 — **대여 기한은 1일(24시간)
  고정**. `docs/PROJECT_GUIDE.md` §8 표를 RESOLVED로 갱신 + §13 변경 이력에 기록. Mock 데이터가
  파일마다 제각각(6시간/12시간/24시간)이었던 걸 전부 `dueAt = rentedAt + 24h`로 통일 —
  `rentalApi.ts`(`ACTIVE_RENTAL` 5시간→23시간, `OVERDUE_UNSETTLED`는 이미 24시간이라 그대로),
  `homeApi.ts`(`ACTIVE_RENTAL`의 `dueAt`/`remainingFreeSeconds` 동일하게 수정),
  `historyApi.ts`(mock-history-002/003의 `dueAt` 재계산 — 003은 기한보다 늦게 반납해서 미정산이 남은
  케이스라는 서사를 유지하기 위해 `endedAt`도 기한 이후 시점으로 같이 조정, 004는 우연히 이미 24시간
  이라 변경 없음, 005는 대여 자체가 실패한 케이스라 해당 없음). `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): `SCR-APP-HISTORY-001`의 `EMPTY` Variant 중 "전체 이용 내역 자체가 없음"(신규
  사용자) 케이스를 별도 예외 화면으로 구현 — 필터별로 0건인 기존 문구(`EMPTY_MESSAGE`)와 달리,
  `filter === 'ALL'`인데도 0건이면 우산 아이콘(`umbrella-outline`) + "아직 이용 내역이 없어요" +
  안내 문구로 더 눈에 띄게 표시(`HistoryScreen.tsx`). 다른 필터(대여중/완료/분실/취소)가 개별적으로
  0건인 경우는 기존처럼 가벼운 텍스트만 유지. 이 상태를 실제로 확인할 수 있도록 `historyApi.ts`에
  다른 Mock 파일들과 동일한 `MOCK_SCENARIO`('WITH_ITEMS'|'EMPTY') 토글 추가. `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): 위 "전체 이용 내역 없음" 예외 화면에 "대여소 찾기" 버튼 추가 — 탭하면 지도 탭
  (`Map`)으로 이동. `HistoryScreen`이 `TabNavigator` 소속 화면이라 `useNavigation<BottomTabNavigationProp<TabParamList>>()`로
  형제 탭(`Map`)에 바로 이동 가능. `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): `NotificationScreen.tsx` 헤더 오른쪽에 "모두 읽음" 버튼 추가 — 로컬 state
  (`markAllRead`)를 참조해야 해서 `RootStackNavigator.tsx`의 정적 `options`가 아니라 화면 안에서
  `useLayoutEffect` + `navigation.setOptions({ headerRight })`로 등록(React Navigation 표준 패턴).
  누르면 현재 로드된 알림 전체의 `alertKey`를 `readKeys`에 한 번에 채워 넣음. `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): `NotificationScreen.tsx`도 이용 내역과 같은 패턴으로 알림을 발생일(`occurredAt`)
  기준 "YYYY년 M월 D일"로 하루씩 묶어 보여주도록 변경(`groupByDay`) — `HistoryScreen.tsx`처럼 별도
  컴포넌트 파일로 쪼개진 않고(항목 렌더링이 상대적으로 단순해서) 화면 안에 그룹핑 함수 + 날짜 헤더만
  추가. `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): 알림 항목별 시각 표시를 절대 날짜("YYYY-MM-DD 오전/오후 H:MM")에서 상대 시간으로
  변경(`formatRelativeTime`, 기존 `formatOccurredAt` 대체) — 1분 미만 "방금 전", 1시간 미만 "N분 전",
  하루 미만 "N시간 전", 딱 하루 전 "어제", 30일 미만 "N일 전", 12개월 미만 "N개월 전", 그 이상은 전부
  "1년 전"(사용자가 지정한 단계 그대로, 2년/3년처럼 더 세분화하지 않음). 같은 김에 `groupByDay`의
  날짜 헤더도 오늘 날짜인 그룹만 "오늘"로 표시하도록 변경. `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): 안 읽은 알림 표시용 노란 점을 카드 왼쪽 상단(flex-row 첫 자식)에서 오른쪽 상단으로
  이동 — 카드를 `relative`로 두고 점을 `absolute right-3 top-3`로 배치, 더 이상 flex 형제가 아니라
  본문 텍스트 블록엔 `pr-4`만 추가해 점 자리와 안 겹치게 함. `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): `SCR-APP-NOTIFICATION-001`의 "알림이 아예 없음" 예외 화면을 `HistoryScreen`과
  같은 톤으로 구현 — 벨 아이콘(`bell-outline`) + "아직 알림이 없어요" + 안내 문구(기존 단순 텍스트
  한 줄에서 업그레이드). 이용 내역과 달리 마땅한 다음 행동이 없어 CTA 버튼은 넣지 않음. 이 상태를
  확인할 수 있도록 `alertApi.ts`에도 다른 Mock 파일들과 동일한 `MOCK_SCENARIO`('WITH_ITEMS'|'EMPTY')
  토글 추가. `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): `AppHeader.tsx`(홈·마이페이지·내역조회 공용 헤더)의 알림 벨 배지를 단순 점
  (`h-2.5 w-2.5`, 숫자 없음)에서 `alertCount` 숫자가 보이는 노란 동그라미 뱃지로 변경 — 10 이상이면
  "9+"로 잘라서 원형 모양이 무너지지 않게 함. `npx tsc --noEmit` 통과 확인.
- 2026-08-05(추가): 사용자 지시로 Phase 재정렬 — 지도+대여소 상세(`SCR-APP-STATION-001` +
  `SCR-APP-STATION-DETAIL-001`, 기존 Phase 8)를 토스 결제 연동(기존 Phase 7)보다 먼저 하기로 순서를
  맞바꿈. `docs/PHASE.md` 갱신(재정렬 이력 노트 추가, 지도가 새 Phase 7·토스 결제가 새 Phase 8).
  `DEC-SF-004`(지도 SDK·위치 권한)는 여전히 미확정 — 지도 착수 전에 반드시 재확인 필요. 문서만
  변경, 코드 변경 없음.
- 2026-08-05(추가): 사용자가 "커스텀 캠퍼스 지도" 스펙을 직접 작성 — `DEC-SF-004`(지도 SDK·위치
  권한) RESOLVED. 실제 지도 SDK(네이버·카카오·Google Maps) 대신 SVG 기반 가상 캠퍼스 지도로 확정,
  신규 라이브러리 설치 없이 기존 `react-native-svg`/`react-native-gesture-handler`/
  `react-native-reanimated`만으로 구현. 이어진 확인 질문 4개 답변 반영 — ① SVG는 간단한 추상적
  플레이스홀더 직접 생성, ② Bottom Sheet는 새 라이브러리 없이 직접 구현, ③ Bottom Sheet가 대여소
  상세 정보의 전부(별도 `SCR-APP-STATION-DETAIL-001` 화면 안 만듦), ④ Mock 대여소 5~8개.
  `docs/PROJECT_GUIDE.md` §8·§13, `docs/PHASE.md` Phase 7 갱신, PLAN
  (`docs/plans/2026-08-05-SCR-APP-STATION-001.md`) 작성 후 구현.
  신규 `src/types/station.ts`(`Station` 타입, 12-R에 `STATION-001` 계약 자체가 없어 잠정 구조임을
  명시)·`src/api/stationApi.ts`(Mock 대여소 7개, 초록(3개 이상)/노랑(1~2개)/빨강(0개) 케이스 골고루
  배치)·`src/assets/campus-map.svg`(추상적 플레이스홀더 — 길·중앙광장·건물 4개 단순 도형).
  `src/screens/SCR-APP-STATION-001/hooks/useCampusMapGesture.ts` — `Gesture.Pinch()`+`Gesture.Pan()`을
  `Gesture.Simultaneous()`로 묶어 배율 1~3배 클램프, 지도가 화면 밖으로 완전히 안 나가게 이동 범위도
  배율에 비례해 클램프. 줌/팬 상태는 React state가 아니라 reanimated shared value로만 관리해 리렌더
  없이 트랜스폼만 갱신(스펙 §7·§9). `StationMarker.tsx` — 우산 개수별 마커 색상, 선택 시 scale 애니메이션
  +그림자로 강조. `StationBottomSheet.tsx` — 새 라이브러리 없이 직접 구현, INT-APP-005 규격(open
  320ms/close 240ms) 재사용, 닫힘 애니메이션이 끝난 뒤에만 실제로 언마운트하도록 `runOnJS`로 마지막
  선택값을 붙잡아두는 패턴 적용. `CampusMap.tsx` — SVG 지도와 마커를 같은 Zoom 트랜스폼 레이어에
  렌더링(스펙 §9), `preserveAspectRatio="xMidYMid slice"`로 SVG가 컨테이너를 항상 꽉 채우게 해서 비율
  좌표(`mapWidth * x`, `mapHeight * y`) 계산이 실제 화면과 어긋나지 않게 함. `MapScreen.tsx` 전체
  재작성 — `selectedStationId`만 화면 레벨 state로 관리(스펙 §7), Loading/Error는 기존 화면들과 동일
  패턴. `app-screen-flow.md` §2.3의 `LOCATION_PERMISSION_DENIED`·`MAP_SDK_ERROR` 등은 실제 GPS/지도
  SDK 전제라 이번 결정과 개념적으로 안 맞음을 문서에 기록(노션 원문보다 사용자 확정 스펙 우선).
  `npx tsc --noEmit` 통과 확인.
- 2026-08-05(버그 수정): 마커 탭 시 Bottom Sheet가 화면 왼쪽 상단에 뜨는 버그 수정 —
  `StationBottomSheet.tsx`가 reanimated `style={sheetStyle}`(transform/opacity)와 NativeWind
  `className="absolute inset-x-0 bottom-0 ..."`을 같은 `Animated.View`에 같이 쓰고 있었는데,
  NativeWind className이 Reanimated의 animated style prop과 안정적으로 안 합쳐져 position 관련
  className이 통째로 소실 → `position:absolute`인데 `left`/`right`/`bottom` 오프셋이 없어 기본값인
  왼쪽 상단에 붙어버린 것. 같은 컴포넌트의 백드롭 `Animated.View`(className 없이 순수 `style`만
  사용)는 정상 동작하던 게 단서. 해결: 시트의 위치·배경·패딩 스타일을 전부 인라인 `style` 배열로
  옮겨 `sheetStyle`과 같은 배열에서 병합(className 의존 제거), 내부의 순수 `View`/`Text`는 그대로
  className 유지. `npx tsc --noEmit` 통과 확인.
- 2026-08-06: 실제 Spring API 연동 착수 — PLAN(`docs/plans/2026-08-06-api-integration.md`) 작성 후 승인.
  범위: 13개 App API 중 이미 화면·Mock이 존재하는 9개(`AUTH-001/002`, `USER-001`, `HOME-001`,
  `RENTAL-001/002`, `SETTLEMENT-001`, `ALERT-001`, `CHATBOT-001`)만 실제 호출로 전환. `PAYMENT-001~003`은
  Phase 8 미착수라 제외, `WEATHER-001`은 `DEC-SF-003` 미확정이라 제외(둘 다 계속 Mock 유지). 사용자가
  `.env`에 `EXPO_PUBLIC_API_BASE_URL` 직접 설정 + `expo-secure-store`/`expo-crypto` 설치 완료(신규
  라이브러리 2개, 사전 승인받음 — 각각 토큰 영속 저장·`X-Request-Id`/UUID 생성용, RN/Hermes에 전역
  `crypto.randomUUID()`가 없어 `expo-crypto` 필요함을 Expo 공식 문서로 확인).
  1단계(배관 작업, 아직 아무 화면에서도 안 씀): 신규 `src/lib/secureStorage.ts`(`expo-secure-store`
  래퍼 — `saveSession`/`loadSession`/`clearSession`, accessToken+expiresAt만 저장)·신규
  `src/api/client.ts`(공용 axios 인스턴스 — 요청마다 `X-Request-Id`(`Crypto.randomUUID()`)와, 토큰이
  있으면 `Authorization: Bearer` 자동 첨부. 토큰 자체는 `client.ts`가 들고 있지 않고 `setAccessToken()`으로
  바깥에서 주입받는 구조(순환 참조 방지). 응답 인터셉터에서 서버 에러 바디의 코드를 뽑아
  `Error.message`로 던져 기존 Mock의 `new Error('INVALID_CREDENTIALS')` 계약 유지, 401(로그인 요청
  자체는 제외)이면 `setUnauthorizedHandler()`로 등록된 콜백 호출). `npx tsc --noEmit` 통과 확인.
  다음 단계: `useAuthStore`에 `hydrate`/`isHydrating` 추가 + `RootNavigator` 부팅 흐름 반영.
- 2026-08-06(추가): 2단계 — `useAuthStore.ts`/`RootNavigator.tsx`에 토큰 영속화 흐름 연결. PLAN
  §6과 달리 부팅 시 `GET /users/me`로 별도 검증하지 않기로 변경(사용자 확인) — `useAuthStore.user`를
  실제로 읽는 화면이 없고(마이페이지·홈이 각자 `useQuery`로 `USER-001`/`HOME-001`을 따로 조회),
  `USER-001` 응답 타입(`MyPageProfile`)과 로그인 응답 타입(`AuthUser`)이 달라 억지로 맞출 필요가
  없다고 판단. 대신 부팅 시 SecureStore에서 토큰만 복원하고, 토큰이 만료돼 있으면 탭 진입 후 첫
  실제 API 호출이 401을 받아 `client.ts`의 `setUnauthorizedHandler` 콜백(`clearAuth`)이 자동 호출되는
  구조로 충분하다고 결론. `useAuthStore`에 `isHydrating`(초기 `true`) 상태와 `hydrate()` 액션 추가 —
  `hydrate()`는 `loadSession()`으로 토큰만 복원하고 끝냄. `setAuth`는 로그인 성공 시
  `saveSession()`으로 SecureStore에 저장, `clearAuth`는 `clearSession()`으로 삭제 + `client.ts`의
  `setAccessToken(null)`도 같이 호출. `client.ts`와의 순환 참조를 피하려고 store 생성 시점에
  `setUnauthorizedHandler(clearAuth)`를 등록(모듈 로드 시 1회). `RootNavigator.tsx`는 마운트 시
  `useEffect`로 `hydrate()` 실행, `isHydrating`인 동안 기존 공용 `LoadingScreen` 컴포넌트 표시.
  `npx tsc --noEmit` 통과 확인. 다음 단계: `authApi.ts`부터 실제 호출로 전환(여기서 실제 서버 에러
  응답 스키마 처음 확인).
- 2026-08-06(추가): 3단계 — `authApi.ts`의 `login`/`logout`을 실제 `POST /api/v1/auth/login`,
  `POST /api/v1/auth/logout` 호출로 교체(Mock 시나리오·지연 상수 제거). 착수 전 실제 서버에 curl로
  직접 호출해 응답 모양을 먼저 확인: 에러 응답은 `{"code":"INVALID_CREDENTIALS","message":"..."}`
  평평한 구조(`client.ts`의 `extractErrorCode`가 이미 `data.code`를 보게 돼 있어 수정 불필요), 성공
  응답도 `data` 래퍼 없이 `LoginResponse` 타입과 완전히 동일한 평평한 구조로 확인(§5 공통 규칙 "응답은
  `data` 래퍼 안에 담김" 문서 설명과 실제 동작이 다름 — 실제 서버 동작 우선, 문서-코드 불일치 기록).
  로그아웃도 발급받은 토큰으로 실제 호출해 `{"loggedOut":true}` 확인. 별개로 로그인 성공 응답의
  `user.displayName`이 깨져서 옴을 발견(`ì œê°ˆí•œë™` 형태) — 요청에 한글을 보내지 않았는데도 깨져 있어
  프론트 원인이 아니라 서버(DB 또는 Spring 직렬화 단계)의 이중 인코딩(UTF-8 바이트를 Windows-1252로
  잘못 디코딩 후 재인코딩) 버그로 판단, 프론트에서 임시로 복원하지 않고 그대로 통과시키기로 함(사용자
  확인) — 백엔드에 별도 리포트 필요. `npx tsc --noEmit` 통과 확인.
- 2026-08-06(추가): 4단계 — `userApi.ts`의 `getMe`를 실제 `GET /api/v1/users/me` 호출로 교체. 실제
  응답 확인 결과 `{userId, displayName, schoolName}`만 오고 `studentNumber`는 없음(문서 §5.3 그대로).
  `MyPageScreen.tsx`가 `profile.studentNumber`를 화면에 렌더링하던 부분을 발견 — 2026-08-04에 Mock에만
  임시로 붙여뒀던 필드(§12 참고)라 실제 연동 시 `undefined`가 뜰 상황이라 확인 후 해당 줄 자체를 화면에서
  제거(사용자 확인). `src/types/user.ts`의 `MyPageProfile`에서도 `studentNumber` 필드 제거. `npx tsc
  --noEmit` 통과 확인.
- 2026-08-06(추가): 5~9단계 — 나머지 6개 API(`homeApi.ts`/`rentalApi.ts`/`historyApi.ts`/
  `settlementApi.ts`/`alertApi.ts`/`chatbotApi.ts`)를 전부 실제 호출로 교체, Mock 시나리오·지연
  상수·목업 데이터 배열 전부 제거. 착수 전 테스트 계정으로 실제 응답을 하나씩 curl/node로 먼저 확인:
  - `HOME-001`(`GET /api/v1/app/home`): `HomeSummary` 타입과 완전히 동일한 평평한 구조 확인, 그대로 연동.
  - `RENTAL-001`(`GET /api/v1/rentals/current`): 활성 대여 없을 때 응답 바디 자체가 문자 그대로 `null`
    (`{"data":null}`이 아님 — §5 공통 규칙의 `data` 래퍼 설명과 다시 한번 다름).
  - `RENTAL-002`(`GET /api/v1/rentals`): `{items, nextCursor}` 그대로, `status`/`cursor`/`size` 쿼리
    파라미터를 실제로 전달하도록 `getRentalHistory`에 `cursor`/`size` 파라미터도 함께 연결(기존엔
    `status`만 실제로 쓰이고 나머지 둘은 Mock이라 무시되고 있었음).
  - `SETTLEMENT-001`(`GET /api/v1/settlements/pending`): `PendingSettlementsResponse`와 동일, 그대로 연동.
  - `ALERT-001`(`GET /api/v1/app/alerts`): 실제 응답이 `{"items":[]}`로 래핑돼 와서(기존 Mock 함수는
    `AlertItem[]`를 직접 반환) `getAlerts`는 시그니처(`Promise<AlertItem[]>`)를 유지한 채 내부에서
    `response.data.items`로 언래핑하도록 구현 — 화면 쪽 변경 없음.
  - `CHATBOT-001`(`POST /api/v1/chatbot/messages`): `ChatbotMessageResponse`와 완전히 동일한 평평한
    구조로 실제 응답(`source:"EXTERNAL_LLM"`) 확인. `sendChatbotMessage`를 실제 호출로 교체하고,
    `ChatbotScreen.tsx`의 `requestId`도 `mock-request-${requestSeq}` → `Crypto.randomUUID()`(expo-crypto)로
    교체(로컬 메시지 리스트 key로 쓰던 `requestSeq` 자체는 서버 전송용이 아니라서 그대로 유지).
  이번 PLAN(`docs/plans/2026-08-06-api-integration.md`) 범위였던 9개 API 전부 실 연동 완료 —
  `PAYMENT-001~003`(Phase 8 미착수)·`WEATHER-001`(`DEC-SF-003` 미확정)은 계획대로 계속 Mock 유지.
  `npx tsc --noEmit` 통과 확인.
- 2026-08-06(추가): `displayName` 이중 인코딩 임시 우회 추가(사용자 요청 — 백엔드와 별도 협의해서
  근본 원인은 나중에 고치기로 함). 신규 `src/lib/mojibake.ts`(`decodeMojibakeDisplayName`) — 원본
  UTF-8 바이트가 Windows-1252로 한 번 잘못 디코딩된 뒤 다시 UTF-8로 재인코딩된 것으로 보여, cp1252의
  0x80~0x9F 특수문자 역매핑 테이블 + Hermes 내장 `TextDecoder`(Expo 공식 문서로 UTF-8 지원 확인, 폴리필
  불필요)로 역변환. **완전한 복원은 아님** — 실제 테스트 계정 이름으로 확인한 결과 중간 글자는
  복원되지만 앞뒤 일부 바이트는 이미 유실돼 대체문자(�)로 남음(정보 유실 구간은 어떤 방식으로도
  복원 불가, 사용자에게 사전 고지 후 그대로 반영하기로 확인). `login`(`authApi.ts`)·
  `getHomeSummary`(`homeApi.ts`)·`getMe`(`userApi.ts`) 3곳에서 `displayName` 필드에만 적용
  (실제 응답 확인 결과 `schoolName` 등 다른 필드는 깨지지 않아 `displayName`에만 한정 적용).
  `npx tsc --noEmit` 통과 확인. 다음: Expo 실행 환경에서 전체 화면(홈·마이페이지·내역조회·알림·챗봇)
  수동 확인.
- 2026-08-06(마무리): 사용자가 Expo 실행 환경에서 홈·마이페이지·내역조회·알림·챗봇 화면을 실제 서버
  데이터로 수동 확인 완료. `docs/plans/2026-08-06-api-integration.md` 범위(9개 API 실 연동 + 토큰
  영속화) 작업 마무리. 남은 후속 작업: (1) `displayName` 서버 인코딩 버그 백엔드 협의(사용자가
  별도 진행 예정), (2) `LoginScreen.tsx`의 자격증명/네트워크 오류 문구 분리(이번 범위에서 제외,
  나중에 별도 진행하기로 함, 2026-08-06 확인).
- 2026-08-06(추가): `WEATHER-001`(`SCR-APP-HOME-001`)도 Mock → 실제 연동으로 전환. 위 작업에서
  제외했던 두 이유(위치 좌표 수집 방법 없음·`DEC-SF-003` 호출 경계 미확정)를 사용자가 이번에 확인해
  해소 — 호출 경계는 다른 API와 동일하게 App→Spring, 좌표는 실제 GPS 대신 캠퍼스 고정값
  (`37.501329, 127.039538`)을 `weatherApi.ts` 상단 상수로 하드코딩. `getShortTermWeather`는
  `GET /api/v1/weather/short-term`을 `latitude`/`longitude` 쿼리로 호출하도록 교체, Mock 지연·목데이터
  제거. `HomeScreen.tsx`·`types/weather.ts`는 함수 시그니처·응답 타입이 그대로라 변경 없음.
  `DEC-SF-003`은 `PROJECT_GUIDE.md` §8에서 RESOLVED로 갱신. 다음: Expo 실행 환경에서 홈 화면 날씨
  위젯 수동 확인 필요.
- 2026-08-06(추가): 초단기 강수 예보 카드의 느낌표 아이콘에 `onPress` 연결(`HomeScreen.tsx`) — 기존엔
  장식용이었음. 로컬 state(`isForecastInfoOpen`)로 토글, 클릭 시 "현재부터 6시간 이내 예보입니다"
  말풍선 툴팁만 표시(사용자 확인 — 예보 항목 자체를 6시간으로 필터링하거나 숨김/노출 전환하는 것은
  아님). 새 공용 Tooltip 컴포넌트는 만들지 않고 이 화면에 인라인으로 구현(다른 화면에서 아직 재사용
  필요 없음).
- 2026-08-06(추가): 날씨 아이콘 선택 기준을 다시 뒤집음(사용자 확인) — 2026-08-04에 확정했던
  "강수확률 구간" 방침 대신, WEATHER-001 실 연동 확인 후 `condition` 필드(`CLEAR|CLOUDY|RAIN|SNOW|
  UNKNOWN`)를 그대로 아이콘에 매핑(`weather-sunny/cloudy/rainy/snowy`, `UNKNOWN`은
  `help-circle-outline`). `weatherIconName`의 인자 타입도 `number`(강수확률)에서
  `WeatherCondition`으로 변경, 호출부(`item.precipitationProbability` → `item.condition`)도 함께 수정.
  `npx tsc --noEmit` 통과 확인.
- 2026-08-06(추가): 챗봇·알림·문의하기 화면 헤더 아래 불필요한 여백 제거 — `Screen` 컴포넌트 기본값
  `edges=['top','right','bottom','left']`가 네이티브 스택 헤더(이미 상단 세이프 에어리어를 자체
  확보함)와 중복으로 top 여백을 한 번 더 잡고 있던 게 원인. 홈·마이페이지가 이미 쓰던 것과 동일하게
  `edges={['right','bottom','left']}`로 top만 빼서 전달(`ChatbotScreen.tsx`, `InquiryScreen.tsx`,
  `NotificationScreen.tsx` — 알림은 에러 상태·정상 렌더 두 곳 모두). `npx tsc --noEmit` 통과 확인.
- 2026-08-06(추가): `AppHeader`의 알림 벨을 홈뿐 아니라 이용 내역 조회·마이페이지에서도 실제로
  `Notification` 화면으로 연결(사용자 요청 — 지금까지는 홈만 연결돼 있었고 나머지 두 곳은 배지만
  표시하고 탭해도 무동작이었음). `HistoryScreen.tsx`(`BottomTabNavigationProp<TabParamList>`)와
  `MyPageScreen.tsx`(`NativeStackNavigationProp<MyPageStackParamList>`)는 각자 하위 네비게이터
  타입만 쓰고 있어서, `NativeStackNavigationProp<RootStackParamList>`로 타입 지정한 `rootNavigation`을
  추가로 얻어 `onPressAlert`에 연결(Home이 이미 쓰던 것과 동일 패턴 — 하위 네비게이터의 navigate가
  상위 스택의 라우트명을 찾을 때까지 자동으로 위로 전파되는 React Navigation 동작 활용). `AppHeader.tsx`
  주석도 갱신. `npx tsc --noEmit` 통과 확인.
- 2026-08-07: 하단 탭 활성 배경 필(pill)에 border-radius가 안 먹던 원인 확인(`TabNavigator.tsx`) —
  `@react-navigation/bottom-tabs`의 `BottomTabItem.tsx`가 활성 배경색에 쓰는 `borderRadius`를
  `variant`(기본값 uikit)별로 자체 계산해 0으로 강제 덮어써서, `tabBarItemStyle.borderRadius`가
  무시되고 있었다(node_modules 소스로 확인). 커스텀 `TabBarButton`의 `Pressable style` 배열 끝에
  `borderRadius: 20`을 명시적으로 다시 얹어 덮어쓰는 방식으로 해결. 필 범위도 넓혀달라는 요청으로
  `tabBarItemStyle`의 `marginHorizontal`(6→4)·`marginVertical`(8→4)을 줄이고 `TabBarButton`
  `height`도 52→56으로 키움. `npx tsc --noEmit` 통과 확인.
- 2026-08-09: `SCR-APP-STATION-001`(지도) `DEC-SF-004` 재번복 — 2026-08-05에 확정했던 SVG 가상
  캠퍼스 지도를 카카오맵 JS SDK 커스텀 타일셋으로 교체(PLAN
  `docs/plans/2026-08-08-SCR-APP-STATION-001.md`). 신규 `react-native-webview` 설치(Expo Go 호환
  확인 완료). 기존 `CampusMap.tsx`/`useCampusMapGesture.ts`(폴더째)/`mapGeometry.ts`/
  `StationMarker.tsx` 삭제, 신규 `KakaoMapWebView.tsx`(RN에서 HTML 문자열 생성 →
  `source={{ html }}`로 WebView 직접 주입)·`kakaoTiles.generated.ts`(타일 base64 임베드). 마커는
  WebView 안 카카오 엔진이 렌더링, 탭 시 `postMessage`로 기존 `StationBottomSheet`(변경 없음) 오픈.
  `stationApi.ts`/`types/station.ts` 계약 변경 없음.
  구현 중 시행착오와 확인된 핵심 사실 세 가지: (1) 카카오 JS SDK는 `source={{ html }}`로 RN에서 만든
  로컬 HTML에서도 도메인/Referer 검증을 통과함을 실기기(iOS)로 확인 — Netlify/Surge/EC2 등 외부
  호스팅이 전혀 불필요해져 애초 PLAN의 배포 단계를 통째로 스킵. (2) `projectionId: null` 좌표계는
  공식 문서상 left-bottom이 원점이고 y가 위로 갈수록 증가하는데, 처음에 이미지 좌표계(y=0이 위)
  그대로 음수(`-y`)를 넣는 실수를 해서 지도 중심이 실제 타일 없는 좌표를 가리켰고, 이게 카카오 실제
  지도(진짜 장소명·워터마크)가 그대로 비쳐 보이는 버그로 이어짐 — 마커 좌표를
  `y = (1 - station.y) * WORLD_SIZE`, 지도 중심을 `Coords(WORLD_SIZE/2, WORLD_SIZE/2)`(양수)로
  수정해 해결. (3) fallback 타일(정의 범위 밖 요청 시 반환)은 투명 PNG로 하면 안 되고 불투명이어야
  함 — 투명이면 그 밑에 깔린 카카오 실제 지도가 노출됨(`TILE_BLANK_FALLBACK`으로 교체).
  타일 구성도 중간에 사용자 지시로 변경 — 처음엔 z0(전체 1장)+z1(2×2 4장) 5장짜리 최소 스코프로
  시작했다가, 카카오 공식 커스텀 타일셋 샘플의 `limit=ceil(3/2^z)` 공식을 그대로 따르는 3단계
  구조(0단계 3×3=9장·최대 확대, 1단계 2×2=4장, 2단계 1×1=1장·최소 확대/전체 지도)로 확장, 총 14장.
  타일 이미지는 사용자가 AI로 초안 생성 후 직접 512×512로 잘라 준비, 원본은
  `src/assets/campus-tiles/`에 보관. 실제 타일 첫 배치 시 이음새·방향이 안 맞는 문제가 있었는데,
  라벨 붙은 디버그 플레이스홀더 14장으로 좌표 계산 자체는 정상임을 먼저 확인한 뒤(격자 배치 문제
  아님), 실제 파일명(`0_0_0.png` 등)에 붙은 x/y가 이미지 내용과 전치(transpose)되어 있던 게
  원인임을 확인 — 사용자가 파일명을 좌표순으로 재배정해 해결. 드래그(팬)는 처음엔 좁은 커버리지
  때문에 막아뒀다가, 최종적으로는 켜두는 쪽으로 확정(2026-08-09 사용자 확인 — 범위 벗어나면 옅은
  회색 빈 화면 노출을 감수하고 자유 이동 UX를 우선). `npx tsc --noEmit` 통과 확인. Expo Go 실기기
  (iOS)로 좌표 정렬·타일 이음새·마커 탭→바텀시트·드래그+핀치줌 전부 최종 확인 완료.
- 2026-08-09(추가): `SCR-APP-AUTH-001`(로그인 화면, Phase 2는 이미 DONE) 공용 `TextField.tsx`에 UX
  개선 2건 — 신규 라이브러리 추가 없음(`@expo/vector-icons`는 기존 SelectField/ConfirmModal 등에서
  이미 쓰던 것 재사용). ① `secureTextEntry` 필드 우측에 눈 아이콘(`eye-outline`/`eye-off-outline`)
  토글 추가 — 로컬 `isRevealed` state로 `secureTextEntry={secureTextEntry && !isRevealed}` 전환.
  ② 값이 있는 단일행 필드(멀티라인 제외 — 문의하기 "내용"처럼 긴 텍스트엔 X 버튼이 어색해서 제외)에
  X(`close-circle`) 지우기 아이콘 추가, 탭하면 `onChangeText('')`. 두 아이콘이 동시에 필요한 경우
  (비밀번호 필드에 값이 있을 때) 겹치지 않게 우측 padding(`pr-11`/`pr-20`)을 아이콘 개수에 따라
  동적으로 계산. 로그인·문의하기(제목) 등 `TextField`를 쓰는 모든 화면에 자동 적용. `npx tsc --noEmit`
  통과 확인.
- 2026-08-09(추가): 로그인 화면 배경색 문의 확인 — 코드는 이미 `background` 토큰(`tailwind.config.js`
  의 `#F5F7FA`)을 쓰고 있어 변경 사항 없음. 흰색처럼 보이는 이유는, 홈/마이페이지/내역조회는 흰
  `AppHeader`와 나란히 있어 회색이 대비로 눈에 띄지만 로그인 화면은 헤더 없이 화면 전체가 단색이라
  대비할 게 없어서로 파악(사용자와 확인 완료, 코드 변경 없음).
- 2026-08-09(추가): `SCR-APP-NOTIFICATION-001`(Phase 6은 이미 DONE) 알림 읽음 처리 UX 개선 — 사용자
  요청으로 "알림을 읽으면 홈/마이페이지/내역 헤더의 알림 배지 숫자도 줄어들게" 변경. 기존엔 읽음
  상태(`readKeys`)가 `NotificationScreen.tsx` 컴포넌트 로컬 `useState`라 화면을 나가면 사라지고,
  헤더의 `alertCount`는 각 화면이 `HOME-001`에서 받아오는 서버 계산값이라 서로 무관했음(설계상 의도된
  동작이었으나 사용자가 연결을 요청).
  신규 `src/store/useAlertReadStore.ts` — Zustand 스토어로 `readKeys: Set<string>` 전역 관리(서버
  영속 저장은 여전히 안 함, `ALERT-002`/`DEC-SF-008` 미확정 그대로). `NotificationScreen.tsx`는 로컬
  `useState` 대신 이 스토어를 사용하도록 교체. `HomeScreen.tsx`/`MyPageScreen.tsx`/`HistoryScreen.tsx`
  3곳 모두 `AppHeader`에 넘기는 `alertCount`를 `Math.max(서버 alertCount - readKeys.size, 0)`로 계산 —
  `alertCount`(홈 API)와 알림 목록 항목 수가 서로 다른 API 소스라 정확히 1:1 대응하는 건 아니라서
  "읽은 개수만큼 차감"하는 근사치임을 사용자에게 사전 고지·확인.
  이어서 "앱을 껐다 켜도 유지됐으면" 요청 반영 — 메모리 전용 상태라 재기동 시 초기화되던 걸 기기
  로컬 영속으로 전환. 저장소로 `expo-secure-store 재사용` vs `@react-native-async-storage/async-storage
  신규 설치` 중 사용자가 후자를 선택(신규 라이브러리 사전 승인, `npx expo install`로 SDK 54 호환 버전
  `2.2.0` 설치). `useAlertReadStore.ts`를 zustand `persist` + `createJSONStorage(() => AsyncStorage)`로
  재작성 — `Set`은 기본 JSON 직렬화가 안 돼 `replacer`/`reviver`로 배열 ↔ Set 상호 변환. `useAuthStore.ts`의
  `clearAuth`에 `useAlertReadStore.getState().reset()` 호출 추가(로그아웃 시 다음 로그인 계정에 이전
  읽음 상태가 섞이지 않도록). `npx tsc --noEmit` 통과 확인.
  이어서 "이 알림을 OS 상단바(상태바 알림)로도 보낼 수 있는지" 질문에는 구현 없이 범위만 설명 —
  (1) 로컬 알림(`expo-notifications`)은 앱이 실행 중이어야 서버를 조회해 띄울 수 있어 완전히 꺼진
  상태에서 자동으로 뜨진 못함(Expo Go는 SDK 53부터 Android 원격 푸시 지원 자체가 빠졌다는 제약도 있음,
  실기기 확인 필요), (2) 진짜 서버 푸시(FCM/APNs)는 기기 Push 토큰 등록 API 등 `12-R` 확정 13개
  API에 전혀 없는 새 서버 기능이 필요해 `DECISION_REQUIRED`에 해당 — 둘 다 이번 요청 범위에서는
  구현하지 않음(코드 변경 없음, 설명만).
- 2026-08-09(추가): 위에서 넣었던 "로그아웃 시 알림 읽음 상태 초기화"를 사용자 요청으로 다시 제거 —
  로그아웃 후 재로그인해도 읽은 알림 개수가 유지되길 원함. `useAuthStore.ts`의 `clearAuth`에서
  `useAlertReadStore.getState().reset()` 호출과 관련 import 제거. `alertKey`가 `"type:domainId"`
  형식이라 계정이 바뀌어도 실질적으로 안전하다고 판단. 이제 아무 데서도 안 쓰이는 `reset` 액션도
  `useAlertReadStore.ts`에서 같이 제거(죽은 코드 방지). `npx tsc --noEmit` 통과 확인.
- 2026-08-09(추가): 알림을 OS 상태바에 로컬 배너로 띄우는 기능 구현 — 이전에 설명만 하고 미뤘던
  "1번(로컬 알림)" 방향. `12-R` 계약에 없는 새 기능이라 `PHASE.md`의 정식 Phase는 아니고, 이미
  완료된 Phase 6(알림)에 대한 추가 UX로 취급(사용자 확인). 진짜 서버 푸시(FCM/APNs, "2번")는
  여전히 구현하지 않음 — `DECISION_REQUIRED`이자 새 서버 기능이 필요해서 범위 밖.
  신규 라이브러리 `expo-notifications`(`npx expo install`로 SDK 54 호환 버전 `~0.32.17` 설치,
  사전 승인받음). 착수 전 Expo 공식 문서(v54)로 API 확인(AGENTS.md 지침) — 로컬 알림은 Expo Go
  SDK 53+ 에서도 계속 지원되고(막힌 건 Android 원격 푸시뿐), `setNotificationHandler`는
  `shouldShowAlert`(deprecated) 대신 `shouldShowBanner`/`shouldShowList` 사용, Android는
  `scheduleNotificationAsync`의 `content`가 아니라 `trigger.channelId`로 채널을 지정해야 하고
  Android 13+는 채널이 미리 있어야 권한 프롬프트 자체가 뜬다는 점을 확인 후 반영.
  신규 `src/lib/notifications.ts` — 모듈 로드 시 `setNotificationHandler`(포그라운드에서도 배너
  노출) 1회 등록, `ensureNotificationPermission()`(Android는 채널 먼저 생성 후 권한 확인/요청),
  `showAlertNotification(item)`(Android는 1초 뒤 `TIME_INTERVAL` 트리거+채널 지정, iOS는
  `trigger: null`로 즉시).
  신규 `src/store/useAlertNotifiedStore.ts` — "이미 배너로 알려준 alertKey" 집합을 기기 로컬
  (AsyncStorage)에 영속(`useAlertReadStore`와 동일 패턴이지만 "읽음"과는 다른 관심사라 별도
  스토어). `hasSeeded` 플래그로 이 기능이 이 기기에서 처음 도는 순간엔 기존 알림 전부를 배너 없이
  조용히 기준선으로만 채워 넣어(`seed()`), 이미 존재하던 알림이 한꺼번에 쏟아지지 않게 함 —
  이후 폴링부터 진짜 새로 생긴 alertKey만 배너 대상.
  신규 `src/hooks/useAlertPolling.ts`(첫 `src/hooks/` 디렉터리) — 로그인 상태일 때만
  `ALERT-001`을 60초 간격(`refetchInterval`, 사용자가 60초/3분/5분 중 선택)으로 다시 조회,
  새 alertKey가 있으면 그때 처음으로 권한 요청 후 순차적으로 배너 표시. `RootNavigator.tsx`에서
  `useAlertPolling(!!accessToken)`으로 로그인 여부에 따라 켜고 끔.
  알려진 한계(사용자에게 사전 고지): 앱이 완전히 종료된 상태에선 폴링 자체가 안 돌아서 동작 안 함,
  포그라운드에서 가장 확실하고 백그라운드는 제한적. 콜드 스타트 시 zustand persist 하이드레이션
  타이밍과 겹치면 아주 드물게 알림 하나를 못 띄울 수 있음(과도한 스팸보다 안전한 실패 방향이라
  가드 로직은 추가하지 않기로 함 — 단순함 우선). `npx tsc --noEmit` 통과 확인. 에뮬레이터가 없어
  실기기 Expo Go에서 권한 프롬프트·배너 노출을 사용자가 직접 확인 필요.
- 2026-08-09(추가): 사용자가 전달한 코드 리뷰 코멘트 3건 검토 후 전부 반영(Phase 2/4는 이미
  DONE — 기존 기능 보완).
  ① `LoginScreen.tsx`의 비밀번호 초기화 시점을 `loginMutation`의 `onSettled`(응답 도착 후, 수백
  ms 뒤)에서 `mutate()` 호출 직후로 앞당김 — `mutate({identifier, password})`에 넘기는 객체는
  호출 시점에 동기적으로 값이 복사되어 진행 중인 요청에 영향이 없으므로 안전하고,
  `PROJECT_GUIDE.md §9.3`의 "제출 직후 즉시 제거"에 더 부합. ② 이메일/비밀번호 `onChangeText`마다
  호출하던 `loginMutation.reset()` 2곳 제거 — 로그인 버튼을 다시 누르면 `mutate()`가 이전 에러
  상태를 자동으로 초기화하므로 불필요한 코드였고, 매 키 입력마다 에러 문구가 너무 일찍 사라지는
  UX 문제도 같이 해결됨(재시도 시점에 자연스럽게 덮어씌워짐). ③ `MyPageScreen.tsx`의
  `logoutMutation`을 `onSuccess: clearAuth` → `onSettled: clearAuth`로 변경 — 로그아웃 API가
  네트워크 오류 등으로 실패해도 로컬 세션은 정리되도록 함(로그아웃은 서버 확인이 아니라 "나가겠다"는
  로컬 의도가 핵심이라는 판단, 에러 배너 추가 대신 이 방향을 선택). `npx tsc --noEmit` 통과 확인.
- 2026-08-09(추가): `HomeScreen.tsx`(Phase 3은 이미 DONE)에 대한 코드 리뷰 코멘트 2건 검토 후
  전부 반영. ① `homeQuery` 실패 시 화면 전체를 에러 화면으로 통째로 반환하던 걸 제거 — 날씨
  (`weatherQuery`)·지도 미리보기(`stationsQuery`)는 `homeQuery`와 무관한 독립 쿼리라(이미 그렇게
  분리해둔 이유가 "실패해도 서로 안 끌고 내려가야 함") 실제로는 실패해도 멀쩡히 보여줄 수 있는데,
  기존 코드는 헤더·ScrollView·RefreshControl까지 통째로 날아가 Pull-to-Refresh도 못 쓰고 날씨도
  못 보는 상태였음. 이제 헤더의 `schoolName`/`alertCount`는 `homeQuery.data?.__ ?? 기본값` 패턴
  (`HistoryScreen.tsx`에서 이미 쓰던 것과 동일)으로 안전하게 처리하고, 인사말 영역만
  `homeQuery.isError`일 때 인라인 `ErrorBanner`+"다시 시도" 버튼으로 바꿔치기 — 날씨·지도는 항상
  그대로 보임(사용하지 않던 `pendingSettlement` 구조분해도 이 김에 같이 제거). ② 아이콘 이름
  타입을 `keyof typeof MaterialCommunityIcons.glyphMap`에서 `ComponentProps<typeof
  MaterialCommunityIcons>['name']`로 교체 — `node_modules`의 실제 타입 정의(`GlyphMap<G> = {[K in
  G]: ...}`)를 확인해보니 두 방식이 결과적으로 완전히 동일한 타입으로 귀결됨을 검증, "더 안전해진다"는
  근거는 없었지만 라이브러리 내부 필드명(`glyphMap`)에 안 기대는 더 일반적인 React 관용구라 스타일
  차원에서 반영. `npx tsc --noEmit` 통과 확인.
- 2026-08-09(추가): 사용자가 전달한 코드 리뷰 코멘트 3건 검토(Phase 4는 이미 DONE). ①
  `MyPageScreen.tsx`의 `rentalQuery` 에러 표시를 회색 `<Text>` 한 줄에서 다른 섹션들과 동일한
  `<ErrorBanner>` + "다시 시도" 버튼(`rentalQuery.refetch()`)으로 교체 — 리뷰가 지적한 "에러인데
  '대여 중인 우산이 없어요'로 잘못 표시된다"는 시나리오는 기존 코드에 이미 `!rentalQuery.isError`
  조건이 있어 실제로는 발생하지 않았지만(검증 후 확인), 에러 상태가 눈에 잘 안 띄고 재시도 수단이
  없다는 실질적인 문제는 남아있어 반영. ② `HomeScreen.tsx`/`MyPageScreen.tsx`/`HistoryScreen.tsx`
  세 화면이 완전히 동일한 `getHomeSummary`(`HOME-001`)를 각각 다른 쿼리 키(`'home'`/
  `'mypage-alert-count'`/`'history-header'`)로 부르고 있던 걸 전부 `'home'`으로 통일 — 탭 전환 시
  TanStack Query 캐시를 공유해 중복 요청 없이 캐시된 값을 즉시 보여주고 백그라운드에서만 갱신되도록
  개선. ③ `ConfirmModal.tsx`에 `disabled?: boolean` prop 추가(확인 버튼 `Pressable`에 실제로
  `disabled` 적용 + `opacity-50` 시각 피드백) — `PROJECT_GUIDE.md §9.1` "전송 중 중복 Tap 차단"을
  실제로 지키기 위함. `InquiryScreen.tsx`가 `disabled={inquiryMutation.isPending}`을 넘기도록
  연결하고, 기존에 `onConfirm` 안에서 하던 `if (!inquiryMutation.isPending)` 수동 체크는 이제
  중복이라 제거(대신 모달 버튼 자체가 막힘). 다른 두 `ConfirmModal` 사용처(마이페이지 연장·신고
  모달)는 `onConfirm`이 동기적으로 모달만 닫는 구조라 영향 없음(prop 기본값 `false`). `npx tsc
  --noEmit` 통과 확인.
- 2026-08-09(추가): 홈 화면 흰 카드에 적용했던 커스텀 그림자(사용자가 `shadow-md` 등 NativeWind
  유틸리티 대신 인라인 `shadowColor`/`shadowOffset`/`shadowOpacity`/`shadowRadius`/`elevation`로
  직접 조정해둔 값)를 내역조회·알림·마이페이지 화면의 카드형 View에도 동일하게 적용. 5개 속성 객체를
  `HomeScreen.tsx`에만 두 번 중복해서 박아뒀던 걸 신규 `src/lib/cardShadow.ts`(`cardShadow` 상수)로
  공용화하고, `HomeScreen.tsx`의 기존 두 곳(날씨 위젯·지도 미리보기)도 이 상수를 쓰도록 같이
  정리. 적용 위치: `HistoryItemCard.tsx`(이용 내역 카드), `MyPageScreen.tsx`(현재 대여 현황 카드·
  문의하기/로그아웃 리스트), `NotificationScreen.tsx`(알림 항목 카드 — 읽음/안읽음 배경색과 무관하게
  둘 다 동일한 그림자 적용, 리스트 안에서 카드 높낮이가 들쭉날쭉해 보이지 않도록). `npx tsc --noEmit`
  통과 확인.
- 2026-08-09(추가): `ChatbotScreen.tsx`(Phase 5는 이미 DONE)에 대한 코드 리뷰 코멘트 3건 검토 후
  전부 반영. ① `handleRetry`에 `mutation.isPending` 가드가 빠져있던 것 발견 — 같은 파일
  `handleSend`/전송 버튼은 이미 이 패턴을 쓰고 있었는데 "다시 시도" 버튼만 빠져있던 명백한 누락.
  `handleRetry` 시작부에 가드 추가 + 버튼에 `disabled={mutation.isPending}` 연결(전송 버튼과 동일한
  `opacity-40` 시각 피드백도 맞춤). ② 새 메시지가 쌓이거나 키보드가 올라올 때 자동으로 맨 아래로
  스크롤되지 않던 문제 수정 — `FlatList inverted`로 바꾸는 대안도 검토했으나 인사말·추천 질문이
  메시지 목록과 한 `ScrollView` 안에 섞여 흐르는 기존 구조를 크게 뜯어고쳐야 해서 위험 대비 이득이
  낮다고 판단, 대신 `ScrollView`에 `ref` 추가 + `onContentSizeChange`(새 메시지 쌓일 때)와
  `Keyboard.addListener('keyboardDidShow', ...)`(키보드 뜰 때) 둘 다에서 `scrollToEnd({ animated:
  true })` 호출하는 방식으로 기존 구조를 유지한 채 보완. ③ 모듈 스코프 `let requestSeq = 0`(사용자
  메시지 React key 생성용) 제거 — 검증 결과 실제로는 계정 간 상태가 섞이거나 key 충돌이 나는 버그는
  아니었지만(값이 항상 유일하고 `messages` 배열 자체가 화면 진입마다 새로 시작함), 컴포넌트 생명주기와
  무관한 전역 변수를 유지할 이유가 없어 정리. 이미 같은 파일에서 서버 `requestId`에 쓰고 있던
  `Crypto.randomUUID()`를 메시지 id에도 그대로 재사용(`useRef` 대안보다 더 단순). `npx tsc --noEmit`
  통과 확인.
- 2026-08-09(추가): `HistoryScreen.tsx`/`NotificationScreen.tsx`(둘 다 Phase 6은 이미 DONE)에 대한
  코드 리뷰 코멘트 3건 중 2건만 선별 반영(사용자 지시로 1건은 반려) — `groupByMonth`/`groupByDay`를
  `useMemo`로 감싸 `items`가 바뀔 때만 재계산하도록 최적화(카드 펼침·읽음 처리 등 무관한 state가
  바뀔 때마다 전체 목록을 다시 그룹화하던 낭비 제거). 두 화면 다 이 훅을 기존 `if (query.isPending)
  return ...` 조건부 return들보다 **먼저** 호출하도록 위치를 옮겨야 했음(Hooks 규칙상 조건부 return
  뒤에 훅을 두면 렌더마다 훅 호출 개수가 달라질 수 있어 위반) — `items`도 `query.data?.items ?? []`
  형태로 이 지점에서 먼저 안전하게 뽑아둠. `NotificationScreen.tsx`는 추가로 `markAllRead`를
  `useCallback`으로 감싸고 `useLayoutEffect` 의존성 배열에 `alertsQuery.data` 대신 `markAllRead`
  자체를 넣음(지금 당장 stale closure 버그는 없었지만, 나중에 이 함수가 다른 state를 참조하도록
  바뀔 때 의존성 배열 누락을 방지하는 예방 차원). `TabNavigator.tsx`의 `rest['aria-selected']` →
  `rest.accessibilityState?.selected` 교체 제안은 **반려** — 설치된
  `@react-navigation/bottom-tabs@7.18.14`의 실제 소스(`BottomTabItem.js:142`)를 확인해보니 이
  버전은 커스텀 `tabBarButton`에 `accessibilityState`를 아예 전달하지 않고 `aria-selected`만
  넘기므로, 리뷰 제안대로 바꾸면 활성 탭 표시가 완전히 사라지는 회귀가 발생함(코드에 이미 남아있던
  2026-08-05 주석과 일치, 재검증만 하고 코드는 그대로 둠). `npx tsc --noEmit` 통과 확인.
- 2026-08-09(추가): 사용자가 전달한 코드 리뷰 코멘트 5건 검토 — 4건은 반려, 1건만 반영. 반려한
  4건: ①·② `StationMarker.tsx`/`useCampusMapGesture.ts`에 대한 지적이었는데 이 두 파일은
  2026-08-09 카카오맵 재구현 때 이미 삭제된 파일이라 반영 대상 자체가 없음(옛날 코드를 보고 작성된
  리뷰로 추정). ③ `KakaoMapWebView.tsx`에 "생략" 주석이 남아 문법 오류를 낸다는 지적 — 실제 파일을
  확인해보니 그런 내용 자체가 없음(사실과 다름). ④ `mojibake.ts`의 `TextDecoder`가 Hermes에서
  `ReferenceError`를 낸다는 지적 — Expo SDK 54 공식 문서 재확인 결과 "TextDecoder는 Hermes에
  UTF-8 인코딩만 내장 지원"하고 이 코드가 정확히 `new TextDecoder('utf-8')`만 쓰고 있어 해당 없음
  (2026-08-06에 이미 실기기로 정상 동작까지 확인된 코드, `PROGRESS.md` 해당 날짜 참고 — 사실과
  다름). 반영한 1건: ⑤ `KakaoMapWebView.tsx`의 `onMessage`에서 `JSON.parse(event.nativeEvent.data)`
  를 감싸는 예외 처리가 없어 WebView가 JSON이 아닌 문자열을 보내면 앱이 죽을 수 있던 문제 —
  `try/catch`로 감싸 파싱 실패 시 로그만 남기고 안전하게 `return`하도록 수정. `npx tsc --noEmit`
  통과 확인.
- 2026-08-09(추가): `HistoryScreen.tsx`(Phase 6은 이미 DONE)의 "대여중" 필터 동작 수정 — 백엔드
  (`RentalAppService.java`)를 확인한 결과 연체는 별도 `Rental.Status`가 아니라 `dueAt` 기준으로
  매번 계산되는 값(`RentalOverdueCalculator`)이라, 연체된 `ACTIVE` 대여는 원래도 "대여중" 필터
  (`status=ACTIVE`)에 포함돼 있었음. 실제로 빠지던 케이스는 `RETURNING`(반납 시도 중)이면서 연체된
  대여 — 상태 Chip 규칙(§9.2, `HistoryItemCard.tsx`)은 `ACTIVE`·`RETURNING`을 같은 "대여중/연체중"
  묶음으로 표시하지만 필터 탭은 서버에 `status=ACTIVE`만 보내 `RETURNING` 항목이 제외됐음. 사용자
  확인으로 프론트 전용 해결(백엔드 미변경) — `historyApi.ts`의 `getRentalHistory` `status` 파라미터
  타입을 `HistoryStatusFilter`(RENTAL-002 §5.6 문서화된 5개 값)에서 `RentalStatus | 'ALL'`로 넓히고,
  `HistoryScreen.tsx`가 "대여중" 필터 선택 시 `useQueries`로 `status=ACTIVE`·`status=RETURNING`을
  병렬 조회해 `rentedAt` 내림차순으로 병합(그 외 필터는 기존과 동일하게 단일 조회). `RENTAL-002` API
  계약 자체(§5.6 문서 값)·`FILTERS`/`EMPTY_MESSAGE` 탭 구성은 변경 없음. PLAN
  `docs/plans/2026-08-09-SCR-APP-HISTORY-001-active-filter.md` 참고. `npx tsc --noEmit` 통과 확인.
