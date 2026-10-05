# PLAN — 실제 Spring API 연동 (Mock → Real)

> 대상: 특정 Screen ID 하나가 아니라, 지금까지 Mock으로 구현된 API 레이어 전체를 가로지르는 인프라 작업.
> 근거: 사용자 지시(2026-08-06, "이제 실제 스프링이랑 api 연동할거야") + 사용자가 대화 중 확인한 사항들:
> - Spring 백엔드에 13개 App Public API 계약 전부 배포됨
> - `.env`는 사용자가 직접 만듦(`BASE_URL=http://52.79.108.195:8080/`) — `EXPO_PUBLIC_` 접두사 필요
>   (Expo 공식 문서 확인: 접두사 없으면 클라이언트 번들에 안 들어감), 변수명은 `frontend/web`의
>   `VITE_API_BASE_URL`과 통일해 `EXPO_PUBLIC_API_BASE_URL`로 변경 예정(사용자가 직접 수정, Claude는
>   `.env` 파일을 건드리지 않음 — CLAUDE.md 규칙)
> - accessToken을 이번에 영속 저장으로 전환(`expo-secure-store`) — `useAuthStore.ts`에 이미
>   2026-07-31 주석으로 예고돼 있던 방향
> - 신규 라이브러리 2개 설치 승인됨: `expo-secure-store`(토큰 영속 저장), `expo-crypto`(UUID 생성 —
>   RN/Hermes에 전역 `crypto.randomUUID()` 없음, Expo 공식 문서로 확인)

---

## 1. 구현 목표

- 지금까지 각 `src/api/*.ts`가 `setTimeout` + Mock 데이터로 흉내 내던 API 호출을, 공용 axios 클라이언트를
  통해 실제 Spring 서버(`EXPO_PUBLIC_API_BASE_URL`)에 호출하도록 전환한다.
- 화면 컴포넌트(스크린 파일)는 건드리지 않는 것을 기본 원칙으로 한다 — 각 `src/api/*.ts`가 내보내는
  함수 시그니처·반환 타입(`src/types/*.ts`)을 그대로 유지하고 내부 구현만 Mock → 실제 호출로 바꾼다.
  (화면에서 이미 TanStack Query `useQuery`/`useMutation`으로 이 함수들을 감싸 쓰고 있어서, 함수 계약만
  지키면 화면 쪽 변경이 필요 없다.)
- accessToken을 `expo-secure-store`로 영속화해서, 앱을 껐다 켜도 로그인이 유지되게 한다.
- 모든 요청에 `X-Request-Id`(UUID), `Accept: application/json` 헤더를 붙이고, 인증이 필요한 요청에는
  `Authorization: Bearer <token>`을 자동으로 붙인다(PROJECT_GUIDE.md §5 공통 규칙).

## 2. 이번에 실제 연동하는 API (9개)

| API | 함수 | 파일 |
|---|---|---|
| `AUTH-001` 로그인 | `login` | `src/api/authApi.ts` |
| `AUTH-002` 로그아웃 | `logout` | `src/api/authApi.ts` |
| `USER-001` 내 계정 조회 | `getMe` | `src/api/userApi.ts` |
| `HOME-001` 홈 요약 | `getHomeSummary` | `src/api/homeApi.ts` |
| `RENTAL-001` 현재 대여 조회 | `getCurrentRental` | `src/api/rentalApi.ts` |
| `RENTAL-002` 이용 내역 조회 | `getRentalHistory` | `src/api/historyApi.ts` |
| `SETTLEMENT-001` 미정산 목록 | `getPendingSettlements` | `src/api/settlementApi.ts` |
| `ALERT-001` 파생 알림 조회 | `getAlerts` | `src/api/alertApi.ts` |
| `CHATBOT-001` 메시지 처리 | `sendMessage` | `src/api/chatbotApi.ts` |

## 3. 이번에 제외하는 것 (그대로 Mock 유지)

| 대상 | 이유 |
|---|---|
| `PAYMENT-001/002/003` | Phase 8(토스 결제) 자체가 아직 미착수(`paymentApi.ts`, 결제 화면, 타입 전부 없음). 지금 실제로
  붙이면 사실상 Phase 8을 앞당겨 구현하는 셈이라 PHASE.md "다음 Phase는 절대 구현하지 않는다" 규칙 위반. |
| `WEATHER-001` | `DEC-SF-003`(날씨 공급자·호출 경계) 여전히 미확정. 실제 호출하려면 위치 좌표(latitude/longitude)
  수집이 필요한데 앱에 위치 권한 흐름 자체가 없음. `DECISION_REQUIRED` 임의 확정 금지 규칙에 따라 계속 Mock(`provider: "UNDECIDED"`).
  → **(2026-08-06 후기)** 같은 날 이후 대화에서 두 사유 모두 해소되어 실제 연동으로 전환됨. 좌표는 캠퍼스
  고정값(`37.501329, 127.039538`) 하드코딩, 호출 경계는 App→Spring으로 확정. 상세는 `PROJECT_GUIDE.md` §8
  `DEC-SF-003` 변경 이력 참고. |
| `STATION-001`(지도), `INQUIRY-001`(문의) | 애초에 12-R 확정 13개 목록에 없는 잠정 API(`DEC-SF-009` 등 미확정). 그대로 유지. |

## 4. 새로 만들 파일

| 구분 | 경로 | 역할 |
|---|---|---|
| 신규 | `src/api/client.ts` | 공용 axios 인스턴스. `baseURL: EXPO_PUBLIC_API_BASE_URL`, 요청 인터셉터(`X-Request-Id` UUID 생성, `Accept`, 토큰 있으면 `Authorization: Bearer`), 응답 인터셉터(에러 바디에서 서버 오류 코드를 뽑아 `Error.message`로 던짐 — 기존 화면들이 `error.message`로 코드 문자열을 기대하는 패턴 유지) |
| 신규 | `src/lib/secureStorage.ts` | `expo-secure-store` 래퍼 — accessToken/expiresAt get·set·remove만 담당하는 얇은 함수 3개 |

## 5. 수정할 파일

| 경로 | 변경 내용 |
|---|---|
| `src/api/authApi.ts` | `login`/`logout` — `client.ts`로 실제 `POST /api/v1/auth/login`, `POST /api/v1/auth/logout` 호출. Mock 시나리오 상수 제거 |
| `src/api/userApi.ts` | `getMe` — 실제 `GET /api/v1/users/me` 호출로 교체 |
| `src/api/homeApi.ts` | `getHomeSummary` — 실제 `GET /api/v1/app/home` 호출로 교체 |
| `src/api/rentalApi.ts` | `getCurrentRental` — 실제 `GET /api/v1/rentals/current` 호출로 교체 |
| `src/api/historyApi.ts` | `getRentalHistory` — 실제 `GET /api/v1/rentals`(쿼리: `status`, `cursor`, `size`) 호출로 교체 |
| `src/api/settlementApi.ts` | `getPendingSettlements` — 실제 `GET /api/v1/settlements/pending` 호출로 교체 |
| `src/api/alertApi.ts` | `getAlerts` — 실제 `GET /api/v1/app/alerts` 호출로 교체 |
| `src/api/chatbotApi.ts` | `sendMessage` — 실제 `POST /api/v1/chatbot/messages` 호출로 교체 |
| `src/screens/SCR-APP-CHATBOT-001/ChatbotScreen.tsx` | `requestId: \`mock-request-${requestSeq}\`` → `Crypto.randomUUID()`(expo-crypto)로 교체 |
| `src/store/useAuthStore.ts` | `accessToken`/`user` 상태는 유지하되, `setAuth`에서 `secureStorage`에 토큰 저장, `clearAuth`에서 삭제. 부팅 시 저장된 토큰을 읽어오는 `hydrate()` 액션과 `isHydrating` 상태 추가 |
| `src/navigation/RootNavigator.tsx` | 부팅 시 `hydrate()` 실행 → 로딩 중엔 스피너, 저장된 토큰이 있으면 `USER-001`로 유효성 확인 후 탭으로, 없거나 401이면 로그인 화면으로 |

## 6. 인증/토큰 처리 설계

- `expo-secure-store`에는 `accessToken`, `expiresAt`만 저장한다(`user` 표시 정보는 저장하지 않고, 앱
  재시작 시 `USER-001`을 다시 호출해 서버 최신 값으로 채운다 — PROJECT_GUIDE.md 원칙상 서버 상태를
  클라이언트가 임의로 캐싱·재계산하지 않는다는 기조와 일치).
- 앱 부팅 흐름: `RootNavigator` 마운트 → `useAuthStore.hydrate()`가 SecureStore에서 토큰을 읽음 →
  토큰 있으면 `client.ts`에 붙여 `GET /api/v1/users/me` 호출 → 성공하면 그 응답으로 `user` 상태 채우고
  탭 화면 진입, 401이면 토큰 삭제 후 로그인 화면.
- 로그인 성공(`AUTH-001`) 시 응답의 `accessToken`/`expiresAt`을 SecureStore에 저장 + `user`는 응답 그대로
  메모리 상태에 반영(추가 호출 없음, 로그인 응답 자체에 이미 `user` 포함).
- 로그아웃(`AUTH-002`) 시 서버 호출 성공 여부와 무관하게 SecureStore·메모리 상태 모두 초기화(§5.2 "만료
  세션 재요청도 true" — 실패해도 로컬 세션은 정리하는 게 안전).
- 비밀번호는 기존 `LoginScreen.tsx`가 이미 `onSettled`에서 즉시 비우고 있어 변경 불필요.

## 7. 오류 처리 설계

- **미해결 사항 — 구현 착수 시 실제 확인 필요**: Spring 오류 응답 바디의 정확한 스키마(예:
  `{ error: { code, message } }`인지 다른 형태인지)가 12-R 문서에 명시돼 있지 않음. 첫 단계로 로그인
  API에 실제로 틀린 비밀번호를 보내 실제 오류 응답 바디를 확인한 뒤, `client.ts`의 응답 인터셉터가
  거기서 `INVALID_CREDENTIALS` 같은 코드 문자열을 뽑아 `throw new Error(code)`로 던지도록 만든다(기존
  Mock이 `reject(new Error('INVALID_CREDENTIALS'))` 형태였고 화면들이 이 패턴을 기대하므로 계약 유지).
- 401(로그인 만료)과 403(권한 부족) 구분(§9.4): 401은 `client.ts` 인터셉터에서 감지해 토큰 정리 +
  로그인 화면으로 강제 이동, 403은 화면단에 그대로 에러로 전달(로그아웃하지 않음).
- `retryable=false` 오류 자동 재전송 금지: TanStack Query 기본값상 `useMutation`은 재시도 0회(로그인·
  챗봇 메시지 등 모두 mutation)라 별도 설정 불필요. `useQuery`류(홈·내역·정산 등 조회)는 기본 3회 재시도가
  걸려 있는데, 이는 네트워크 일시 오류에 대한 재조회이지 사용자 액션 재실행이 아니라서 §9.4 취지에
  어긋나지 않는다고 보고 별도 변경하지 않음.
- 기술 오류·stack trace·token은 어떤 로그에도 남기지 않는다 — `client.ts`에 요청/응답 바디를 통째로
  `console.log`하는 디버그 코드를 넣지 않는다.

## 8. 알려진 리스크 / 확인 필요

- `LoginScreen.tsx`가 현재 모든 오류를 "계정 정보를 확인해주세요"로 뭉뚱그려 표시 중(§4.1 "자격 증명
  오류와 네트워크·서버 오류를 분리" 원칙과 어긋남). Mock 환경에선 네트워크 오류가 날 수 없어 문제가
  안 됐지만, 실제 서버 연동 후엔 네트워크 오류도 발생 가능 — 이번 작업 범위에 이 문구 분리 수정을
  포함할지 확인 필요.
- Expo Go로 물리 기기에서 테스트할 경우 `52.79.108.195:8080`이 사설 IP가 아니라 공인 IP로 보여서
  네트워크 접근 자체는 문제없어 보이나, HTTP(비-HTTPS)라 향후 실제 배포 시 iOS App Transport Security
  등에 걸릴 수 있음 — 지금 단계(Expo Go 개발)에서는 영향 없음.
- `client.ts` 도입 후 기존 각 API 파일의 `MIN_DELAY_MS`/`MAX_DELAY_MS`(가짜 지연) 상수는 제거.

## 9. 작업 순서 (커밋은 사용자 승인 후 진행)

1. `npx expo install expo-secure-store expo-crypto` (사용자가 직접 실행 완료)
2. `.env`를 `EXPO_PUBLIC_API_BASE_URL`로 변경(사용자가 직접 수정)
3. `src/lib/secureStorage.ts`, `src/api/client.ts` 작성
4. `useAuthStore.ts`에 `hydrate`/`isHydrating` 추가, `RootNavigator.tsx` 부팅 흐름 반영
5. `authApi.ts`(로그인 먼저 실제 연동 후 수동 확인 — 여기서 실제 오류 응답 스키마 확인) → 나머지 8개
   API 순서대로 교체
6. `ChatbotScreen.tsx`의 `requestId`를 `Crypto.randomUUID()`로 교체
7. 각 단계마다 Expo 실행 환경에서 수동 화면 확인
8. `PROGRESS.md` 갱신
