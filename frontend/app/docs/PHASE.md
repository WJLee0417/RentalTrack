이 문서는 현재 구현해야 하는 기능 범위를 정의한다.

AI는 반드시 현재 Phase만 구현하며, 다음 Phase의 기능을 미리 구현하거나 추측하지 않는다.

> Phase 구성은 P0 확정 화면(PROJECT_GUIDE.md §4)을 기준으로 한다. 2026-07-31 논의로
> (1) MyPage의 Toss 결제 연동을 별도 Phase로 분리하고, (2) 챗봇 순서를 Auth·Home 바로
> 다음(MyPage/History보다 먼저)으로 앞당겼다.
>
> 2026-08-04(추가 재정렬): 기존 "Phase 8(범위 미정)"에 묶여 있던 지도·대여소 상세·알림·문의를
> 사용자 지시로 다시 나눴다 — (1) 문의(`INQUIRY-001`)는 마이페이지 조회와 함께 묶어서 진행,
> (2) 지도(`STATION-001`)와 대여소 상세(`STATION-DETAIL-001`)는 서로 묶은 채 맨 마지막
> (Phase 9)으로 미룬다. `DEC-SF-004`(지도 SDK)·`DEC-SF-008`(알림 영속성)·`DEC-SF-009`
> (문의 저장)가 여전히 미확정이라는 사실 자체는 바뀌지 않는다 — 순서만 재배치.
>
> 2026-08-04(재정렬 수정): 마이페이지(조회)+문의 묶음과 알림 순서를 사용자 지시로 다시 맞바꿨다 —
> 마이페이지(조회)+문의가 Phase 4(챗봇보다 먼저), 알림은 Phase 6(챗봇 다음)으로 이동.
>
> 2026-08-05(재정렬): 알림(`SCR-APP-NOTIFICATION-001`)과 이용 내역 조회(`SCR-APP-HISTORY-001`, 기존
> Phase 8)를 사용자 지시로 하나의 Phase(새 Phase 6)로 묶었다. 토스 결제 연동은 그대로 다음 순서
> (Phase 7)로 밀리고, 지도+대여소 상세(기존 Phase 9)는 빈 자리로 당겨져 Phase 8이 됐다. `DEC-SF-004`
> (지도 SDK)·`DEC-SF-008`(알림 영속성)이 여전히 미확정이라는 사실 자체는 바뀌지 않는다 — 순서만 재배치.
>
> 2026-08-05(추가 재정렬): 사용자 지시로 지도+대여소 상세(기존 Phase 8)를 토스 결제(기존 Phase 7)보다
> 먼저 하기로 순서를 맞바꿨다 — 지도가 새 Phase 7, 토스 결제가 새 Phase 8. `DEC-SF-004`(지도 SDK·위치
> 권한)는 여전히 미확정이라 실제 착수 전에 반드시 재확인이 필요하다(§8 DECISION_REQUIRED 표 참고).

## Phase 1
### SCR-APP-COMMON

목표

- React Native(Expo) 프로젝트 초기화 및 App 공통 UI 기반 구축

구현 범위

- Expo 프로젝트 초기화(`npx create-expo-app` 등, 정확한 커맨드는 착수 시 팀 확인)
- 공통 레이아웃(Safe Area 대응)
- 공통 Button/Input/로딩/오류 컴포넌트
- 공통 색상·타이포그래피 토큰 (Kiosk와 공유 토큰 있으면 맞춤 확인 필요 — PROJECT_GUIDE.md에 실제 값 없음, 별도 확인)
- Zustand 스토어 초기 설정
- 하단 탭 내비게이션 뼈대(Home/History/MyPage/Chatbot 등 실제 라우팅 구조는 착수 시 확정)

완료 후

- PROGRESS.md 업데이트 ✅

상태

✅ DONE (2026-07-31 — 공통 컴포넌트·NativeWind 설정·하단 탭 4개 완료. 탭 구성은 사용자 와이어프레임 확인 후
홈/지도/내역조회/마이페이지로 정정, 챗봇은 탭 대신 홈 화면 플로팅 버튼으로 추후 연결 예정. 상세는
PROGRESS.md·PROJECT_GUIDE.md §12 참고)

---

## Phase 2
### SCR-APP-AUTH-001

목표

사용자는 대학 계정으로 로그인/로그아웃할 수 있다.

구현 범위

- 로그인 화면(`DEFAULT`, `INVALID_CREDENTIALS`)
- `AUTH-001`(로그인), `AUTH-002`(로그아웃) 연동(Mock 우선)
- 토큰 저장·세션 관리(구체적 저장 방식은 착수 시 확정 — 미확정 상태로 임의 구현 금지)
- 비밀번호 찾기는 P0에서 숨김(구현하지 않음) — 와이어프레임 1~2번 페이지엔 "비밀번호를 잊으셨나요?" 링크가
  그려져 있었지만, 사용자가 이 항목은 **노션 문서(`DEC-SF-002`) 기준으로 확정**(2026-07-31) — 링크 미구현.

상태

✅ DONE (2026-08-04 — 로그인 화면(`DEFAULT`/`INVALID_CREDENTIALS`), `AUTH-001`/`AUTH-002` Mock 연동,
`RootNavigator`의 인증 상태 기반 분기(비로그인 시 로그인 화면만 마운트) 완료. 토큰은 Zustand 메모리에만
보관(영속 저장 없음 — 착수 시 미확정 상태 유지 결정). 이후 서버 상태 관리 라이브러리로 `@tanstack/react-query`
확정 도입, 로그인/로그아웃 호출을 `useMutation` 기반으로 리팩터링. 상세는 PROGRESS.md 참고)

---

## Phase 3
### SCR-APP-HOME-001

목표

사용자는 홈에서 현재 대여·미정산 요약을 확인할 수 있다.

구현 범위

- 홈 화면 UI
- `HOME-001` 연동(Mock 우선)
- `WEATHER-001`은 `DEC-SF-003` 미확정이므로 화면 자리만 두고 실제 공급자 연동은 보류 가능성 있음 — 착수 시 재확인
- 부분 성공 처리(날씨 실패가 대여·정산 표시를 막지 않음)

상태

✅ DONE (2026-08-04 — 홈 화면 UI 구현. `HOME-001` Mock(`src/api/homeApi.ts`, 시나리오 전환 가능) 연동,
사용자 정보·미정산 요약·알림 배지·Pull-to-Refresh 구현. 이후 사용자가 실제 목업 스크린샷을 공유해 레이아웃을
목업 기준으로 재조정(같은 날 추가 반영): `WEATHER-001`은 `DEC-SF-003` 미확정이지만 Mock 데이터 기반 상세
위젯(시간대별 강수확률 + 우산 추천 배너, `provider:"UNDECIDED"`)으로 구현(실제 공급자 호출은 여전히 없음).
중앙 영역(GAP-004 "현재 상태·지도 조합")은 지도 SDK 미확정(`DEC-SF-004`)이라 당장은 빈 박스만 두기로 결정
(현재 대여 요약 카드는 이번엔 빠짐, 재배치는 추후). 챗봇 플로팅 버튼은 자리만 배치하고 `onPress`는
no-op(챗봇 Phase — 재정렬 후 Phase 5 — 에서 실제 연결). 상세는 PROGRESS.md·
`docs/plans/2026-08-04-SCR-APP-HOME-001.md` 참고)

---

## Phase 4
### SCR-APP-MYPAGE-001 (조회 전용) + SCR-APP-INQUIRY-001

목표

사용자는 마이페이지에서 현재 대여·미정산을 조회할 수 있다. (결제 자체는 Phase 7) 문의 접수는
2026-08-04 사용자 지시로 이 Phase에 함께 묶는다.

> 2026-08-04(재정렬 수정): 원래 알림과 순서가 맞바뀌어 이 Phase가 챗봇보다 먼저(Phase 4)로 왔다.

구현 범위

- 마이페이지 화면(`ACTIVE_RENTAL`, `OVERDUE_UNSETTLED`) — 조회만
- `USER-001`, `RENTAL-001`, `SETTLEMENT-001` 연동(Mock 우선)
- 결제 버튼은 UI만 두고 실제 결제 흐름은 Phase 7에서 연결
- 연장(`EXTENSION_CONFIRM`)·신고(`INCIDENT_REPORT_CONFIRM`)는 `DEFERRED_NOT_CONTRACTED` — 확인 모달·API는
  구현하지 않음(실제 목업 확인 후 사용자 지시로 버튼 자체는 no-op으로 자리만 배치, 2026-08-04)
- 문의(`SCR-APP-INQUIRY-001`) — `DEC-SF-009`(문의 저장 방식) 미확정이라, 착수 시 네이티브 폼(`INQUIRY-001`
  API) vs 외부 링크/이메일 축소(`NO_API_REQUIRED`) 중 무엇으로 할지 다시 확인 필요

상태

✅ DONE (2026-08-04 — 마이페이지 실제 UI 구현. `USER-001`/`RENTAL-001`/`SETTLEMENT-001` Mock(각각
`userApi.ts`/`rentalApi.ts`/`settlementApi.ts`, 독립 쿼리로 부분 실패 허용) 연동. 실제 목업 스크린샷 확인
후 반영: 프로필(아바타 이니셜+이름+학번), 현재 대여 카드(상태 Chip·진행바·남은 시간, 연체 시 연체일수+연체료
표시), 연장하기·신고하기 버튼(자리만 배치, no-op), 미정산 카드(있을 때만, 결제 버튼은 Phase 7 연결 예정
no-op), 문의하기·로그아웃 리스트. 학번은 `USER-001` 공식 응답엔 없는 필드지만 목업 확인 후 사용자 승인으로
Mock에 한해 추가(`src/types/user.ts` 주석 참고). 문의하기(`SCR-APP-INQUIRY-001`)는 `DEC-SF-009` 미확정
상태에서 사용자가 네이티브 폼 방식으로 결정 — 유형/제목/내용 입력 + Mock 제출(`inquiryApi.ts`, 12-R
미확정이라 잠정 구조임을 명시). 마이페이지 탭에 `MyPageStackNavigator`(네이티브 스택) 추가해
`MyPageHome ↔ Inquiry` 이동 구현. `TextField`에 `multiline` 옵션 추가. `npx tsc --noEmit` 통과 확인)

✅ 2026-08-04(추가): 실제 와이어프레임(Extend modal/Frame 13) 참고해 연장하기·신고하기 버튼을
no-op에서 **확인 모달(Mock)** 로 업그레이드 — `DEC-SF-006`/`DEC-SF-007`은 여전히 `DEFERRED_NOT_CONTRACTED`
라 모달 확인 버튼을 눌러도 실제 서버 반영은 없음(닫히기만 함). 공용 `ConfirmModal.tsx` 신규(두 모달+
문의하기 제출 확인에서도 재사용). 문의하기(`SCR-APP-INQUIRY-001`) 제출 흐름도 "제출 후 전체화면 성공
문구" 방식에서 "제출 **전** ConfirmModal 확인" 방식으로 변경. 마이페이지·홈 공용 헤더를 `AppHeader.tsx`로
분리(로고+학교명+알림 배지, 상태바까지 이어지는 edge-to-edge 흰 배경). Prettier 도입(devDependency +
`.prettierrc.json` + `.vscode/settings.json`). 상세는 `PROGRESS.md` 참고. 새 API·타입 추가 없음(계약
범위 변동 없음) — Phase 4 실질 범위는 그대로 유지.

---

## Phase 5
### SCR-APP-CHATBOT-001

목표

사용자는 빌리 챗봇에게 이용 관련 질문을 하고 안내를 받을 수 있다.

> 09번 문서가 "App 유일의 명확한 P0 확정 신규 기능"으로 강조하는 화면이다. 마이페이지(조회)
> 다음 순서로 유지된다.

구현 범위

- 챗봇 화면(`INITIAL`, `ANSWERING`, `ANSWERED`, `FALLBACK_FAQ`, `TIMEOUT`, `ERROR`)
- `CHATBOT-001` 연동(Mock 우선)
- 허용 CTA 5종만 구현(`OPEN_MAP | OPEN_MY_PAGE | OPEN_HISTORY | OPEN_INQUIRY | NONE`) — Phase 4 완료로
  `OPEN_MY_PAGE`/`OPEN_INQUIRY`는 실제 라우팅 연결 가능. `OPEN_HISTORY`(Phase 6)·`OPEN_MAP`(Phase 8)는
  아직 대상 화면이 없으니 자리만 마련하고 실제 라우팅은 해당 화면 Phase 완료 후 연결
- 대화 이력은 화면 세션 메모리에만 유지(영속 저장 금지)

상태

✅ DONE (2026-08-05 — PLAN(`docs/plans/2026-08-05-SCR-APP-CHATBOT-001.md`) 승인 후 구현. 홈 화면 챗봇
플로팅 버튼이 탭 바깥 화면으로 진입할 방법이 없어서 `RootStackNavigator.tsx`(신규)로 `Tabs ↔ Chatbot`
구조를 추가(`RootNavigator.tsx`가 `TabNavigator` 대신 이걸 렌더링). `Chatbot`은 `Inquiry`와 동일하게
네이티브 기본 헤더 + `headerBackButtonDisplayMode: 'minimal'`로 뒤로가기(사용자 확인, 2026-08-05).
`CHATBOT-001` Mock(`src/api/chatbotApi.ts`, `MOCK_SCENARIO`로 정상/폴백/타임아웃/에러 전환) + 타입
(`src/types/chatbot.ts`, Intent 7종·Action 5종·Source 3종·오류 코드 7종 모두 §5.13/§6 원문 값만 사용).
화면 6개 Variant 전부 구현: `INITIAL`(추천 질문 6개, P0 Intent 1:1 대응)/`ANSWERING`/`ANSWERED`(CTA 버튼
포함)/`FALLBACK_FAQ`(절제된 보조 문구만 표시)/`TIMEOUT`/`ERROR`(입력 보존 + 다시 시도·문의하기). CTA는
`OPEN_MY_PAGE`·`OPEN_INQUIRY`만 실제 라우팅(`TabParamList`의 `MyPage`를
`NavigatorScreenParams<MyPageStackParamList>`로 바꿔 중첩 네비게이션 타입 지원), `OPEN_HISTORY`·
`OPEN_MAP`은 버튼만 두고 라우팅 없음. 대화 이력은 컴포넌트 로컬 state에만 유지. `CHATBOT_MESSAGE_TOO_LONG`
`CHATBOT_UNSAFE_INPUT`·`CHATBOT_RATE_LIMITED` 오류 코드는 타입만 정의하고 Mock 시나리오로는 구현하지
않음(요청 범위 밖, 필요 시 추후 추가). `npx tsc --noEmit` 통과 확인)

---

## Phase 6
### SCR-APP-NOTIFICATION-001 + SCR-APP-HISTORY-001

목표

사용자는 알림 목록을 확인하고, 이용 내역을 조회할 수 있다.

> 2026-08-04(재정렬 수정): 마이페이지(조회)+문의와 순서가 맞바뀌어 챗봇 다음(Phase 6)으로 왔다.
> `DEC-SF-008`(알림 영속성)은 여전히 미확정이므로, 이번 Phase에서도 서버 읽음 저장 API
> (`ALERT-002`)는 만들지 않는다 — 읽음 상태는 App 로컬 상태로만 처리(PROJECT_GUIDE.md §5.11
> 원칙 그대로).
>
> 2026-08-05(재정렬): 원래 Phase 8이었던 이용 내역 조회(`SCR-APP-HISTORY-001`)를 사용자 지시로
> 이 Phase에 합쳤다. 두 화면은 서로 다른 Screen ID·API(`ALERT-001` / `RENTAL-002`)라 각각 독립적으로
> 구현하되, 완료 순서는 자유(둘 다 이 Phase 안에서 마쳐야 다음 Phase로 넘어감).

구현 범위

**알림(`SCR-APP-NOTIFICATION-001`)**
- 알림 목록 화면(`LIST_WITH_UNREAD`, `EMPTY`, `SWIPE_ACTION` — app-screen-flow.md §2.7)
- `ALERT-001`(목록 조회) 연동(Mock 우선) — `alertKey`, `type`, `title`, `message`, `occurredAt`,
  `read`(로컬), `action{type, targetId}` 구조 그대로
- 읽음 처리는 로컬 상태에서만(서버 영속 저장 없음), 새로고침 시 초기화되는 것은 정상 동작
- 홈 화면의 알림 벨(현재 no-op 배지)을 이 화면으로 실제 연결
- 서버 영속 읽음 저장(`ALERT-002`)은 여전히 `DEFERRED_NOT_CONTRACTED` — 구현하지 않음

**이용 내역 조회(`SCR-APP-HISTORY-001`)**
- 이용 내역 화면(`ALL`, `ACTIVE`, `COMPLETED`, `EMPTY`, `CARD_EXPANDED`)
- `RENTAL-002` 연동(Mock 우선, cursor 기반 페이지네이션)
- 챗봇 화면의 `OPEN_HISTORY` CTA를 이 화면으로 실제 연결(Phase 5에서는 대상 화면이 없어 자리만 둠)

상태

✅ DONE (2026-08-05 — PLAN(`docs/plans/2026-08-05-SCR-APP-NOTIFICATION-001-HISTORY-001.md`) 승인 후 구현.
신규 `src/types/history.ts`/`src/api/historyApi.ts`(`RENTAL-002` Mock, status 필터·`nextCursor`는 항상
`null`) + `HistoryScreen.tsx` 전체 재작성 — 필터 탭 5개(전체/대여중/완료/분실/실패, status 쿼리 enum
그대로 노출, 사용자 확인), 카드 탭으로 펼침(`CARD_EXPANDED`), 상태 Chip(§9.2 재사용, 새 Enum 없음)·
정산 뱃지, Home/MyPage와 동일한 공용 `AppHeader` 재사용(alertCount는 `HOME-001` 재사용, 전용 API 안 만듦).
신규 `src/types/alert.ts`/`src/api/alertApi.ts`(`ALERT-001` Mock, `read`는 응답에 없고 화면 로컬 state로만
관리) + `NotificationScreen.tsx` 신규 작성 — `RootStackNavigator`에 `Chatbot`과 동일한 방식으로 등록.
읽음 처리(`SWIPE_ACTION`)는 사용자 확인으로 실제 스와이프 제스처로 구현 — `react-native-gesture-handler`
(Expo SDK 54 호환 `~2.28.0`) 신규 설치 승인받고 추가, `App.tsx`를 `GestureHandlerRootView`로 감쌈.
홈 화면 알림 벨을 `AppHeader`의 신규 `onPressAlert` prop으로 `Notification` 화면에 실제 연결(마이페이지
알림 벨은 이번 Phase 범위 아니라 그대로 둠). 챗봇의 `OPEN_HISTORY` CTA도 `Tabs → History`로 실제
연결. `ALERT-002`(서버 영속 읽음)는 여전히 구현 안 함. `npx tsc --noEmit` 통과 확인)

---

## Phase 7
### SCR-APP-STATION-001 (커스텀 캠퍼스 지도)

목표

지도 화면 — 2026-08-04 사용자 지시로 대여소 상세와 묶어 맨 마지막 Phase로 미뤘다가, 2026-08-05
재정렬로 이용 내역 조회가 Phase 6으로 옮겨가며 Phase 8로 한 번 당겨졌고, 2026-08-05(추가 재정렬)에
사용자 지시로 Toss 결제보다 먼저 하기로 하며 다시 Phase 7로 당겨졌다.

> 2026-08-05(범위 확정): `DEC-SF-004`(지도 SDK·위치 권한) RESOLVED — 실제 지도 SDK(네이버·카카오·
> Google Maps) 없이 SVG 기반 가상 캠퍼스 지도로 확정(사용자가 직접 작성한 스펙 기반, PLAN
> `docs/plans/2026-08-05-SCR-APP-STATION-001.md` 참고). 마커 탭 시 나오는 Bottom Sheet가 대여소
> 상세 정보의 전부라 별도 `SCR-APP-STATION-DETAIL-001` 화면은 이번 Phase에서 만들지 않기로 확정
> (제목에서도 뺌). `app-screen-flow.md` §2.3의 `LOCATION_PERMISSION_DENIED`·`MAP_SDK_ERROR` 등은
> 실제 GPS/지도 SDK 전제라 이 결정과 개념적으로 안 맞음 — 노션 원문보다 이번 사용자 확정 스펙을
> 우선한다(PROJECT_GUIDE.md §13 변경 이력에도 기록).

구현 범위

- SVG 캠퍼스 지도 + Pinch Zoom(1~3배)·Pan(이동 범위 제한)
- 대여소 마커(우산 개수별 색상: 3개 이상 초록/1~2개 노랑/0개 빨강), 선택 시 확대+그림자 강조
- 마커 탭 → Bottom Sheet(새 라이브러리 없이 직접 구현, INT-APP-005 규격 재사용)로 대여소 이름·
  남은 개수·설명 표시
- Mock 대여소 5~8개, `GET /stations` 계약이 나중에 생겨도 교체하기 쉬운 구조로 설계
- 신규 라이브러리 설치 없음(`react-native-svg`/`react-native-gesture-handler`/`react-native-reanimated`
  기존 설치분 재사용)

상태

✅ DONE (2026-08-05 — PLAN(`docs/plans/2026-08-05-SCR-APP-STATION-001.md`) 승인 후 구현. 신규
`src/types/station.ts`(`Station` 타입 — 12-R에 `STATION-001` 계약 자체가 없어 잠정 구조임을 명시)·
`src/api/stationApi.ts`(Mock 대여소 7개, 초록/노랑/빨강 케이스 골고루 배치)·
`src/assets/campus-map.svg`(추상적 플레이스홀더 — 길/광장/건물 4개 단순 도형)·
`src/screens/SCR-APP-STATION-001/hooks/useCampusMapGesture.ts`(Pinch+Pan → scale/translate
reanimated shared value, React state 아님, 배율·이동 범위 클램프)·`StationMarker.tsx`(색상+선택
애니메이션)·`StationBottomSheet.tsx`(직접 구현, 닫힘 애니메이션이 끝난 뒤에만 언마운트하도록
`runOnJS`로 마지막 값 유지)·`CampusMap.tsx`(SVG+마커를 같은 Zoom 트랜스폼 레이어에 렌더링,
`preserveAspectRatio="xMidYMid slice"`로 컨테이너 전체를 채워 비율 좌표 계산과 어긋나지 않게 함).
`MapScreen.tsx` 전체 재작성 — `selectedStationId`만 화면 레벨 state로 관리. `npx tsc --noEmit` 통과 확인)

✅ 2026-08-09(재구현): `DEC-SF-004`를 사용자 지시로 재번복 — SVG 가상 캠퍼스 지도 대신 **카카오맵
JS SDK 커스텀 타일셋**으로 교체(PLAN `docs/plans/2026-08-08-SCR-APP-STATION-001.md`). 신규
`react-native-webview` 설치(Expo Go 호환 확인). `CampusMap.tsx`/`useCampusMapGesture.ts`(폴더째)/
`mapGeometry.ts`/`StationMarker.tsx` 삭제, 신규 `KakaoMapWebView.tsx`(HTML을 RN에서 문자열로 생성해
`source={{ html }}`로 WebView에 직접 주입 — 외부 호스팅 불필요함을 실기기로 확인)·
`kakaoTiles.generated.ts`(타일 14장 base64 임베드). 마커는 WebView 안 카카오 엔진이 렌더링하고 탭
시 `postMessage`로 기존 `StationBottomSheet`(네이티브, 변경 없음) 오픈. `stationApi.ts`/
`types/station.ts` 계약 변경 없음. 실기기 디버깅 과정에서 확인된 핵심: `projectionId: null`
좌표계는 left-bottom이 원점이라 이미지 좌표(y=0이 위)를 뒤집어야 하고, fallback 타일은 불투명이어야
카카오 실제 지도가 비쳐 보이지 않음. 타일 14장(0단계 3×3·1단계 2×2·2단계 1×1, `limit=ceil(3/2^z)`)은
사용자가 직접 제작, 원본은 `src/assets/campus-tiles/`에 보관. 드래그(팬)는 켜두기로 확정(2026-08-09
사용자 확인 — 정의된 범위를 벗어나면 옅은 회색 빈 화면 노출을 감수). `npx tsc --noEmit` 통과 확인.

상태

✅ DONE (2026-08-09, 카카오맵 커스텀 타일셋 방식으로 재구현 완료)

---

## Phase 8
### SCR-APP-MYPAGE-001 (Toss 결제 연동)

목표

사용자는 미정산 금액을 Toss로 결제할 수 있다.

> MyPage 조회(Phase 4)와 분리한 이유: 외부 결제창 이동·딥링크 복귀·Idempotency-Key
> 관리 등 리스크가 다른 화면과 다르고, 독립적으로 집중 검증할 필요가 있다.
> 2026-08-05(재정렬): 지도(기존 Phase 7)가 사용자 지시로 먼저 순서를 가져가며 이 Phase가 8로 밀렸다.

구현 범위

- 결제 시도 생성(`PAYMENT-001`) → Toss 테스트 결제창 이동
- 딥링크 복귀 처리(`successRedirectUri`/`failRedirectUri`, 정확한 스킴 값은 착수 시 확인)
- 결제 승인(`PAYMENT-002`) 또는 상태 재조회(`PAYMENT-003`)
- 복귀 후 화면 문구만으로 성공 확정 금지 — 반드시 서버 최종 상태 재조회

상태

⏸ WAITING (미착수)

---

# Rules

- 현재 CURRENT PHASE만 구현한다.
- 다음 Phase는 절대 구현하지 않는다.
- Mock API만 사용한다(Spring 서버 실연동 전까지).
- DECISION_REQUIRED 항목은 구현하지 않는다.
- 화면 ID는 app-screen-flow.md 기준으로 사용한다.
- 완료 후 PROGRESS.md를 갱신한다.
- 완료 후 다음 Phase로 CURRENT PHASE를 변경한다.
