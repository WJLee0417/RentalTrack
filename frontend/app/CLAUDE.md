# Billisan App - Project Rules

## Must Read Before Coding
- Read @docs/PROJECT_GUIDE.md before implementation (팀 프로젝트 지침서)
- Read @docs/app-screen-flow.md for the current phase's Screen ID section
- Read @docs/PHASE.md and implement only the current phase
- Read @AGENTS.md before writing any Expo/React Native API 관련 코드 — Expo SDK 버전별
  문서를 반드시 확인하라는 Expo 공식 안내다(SDK 버전은 package.json의 `expo` 참고).
- If a required doc is missing or empty, stop and ask — do not guess or proceed without it

## Project Overview
빌리산(Billisan) — 얼굴 온디바이스 인증 기반 공유 우산 대여 서비스의 사용자 모바일 앱.
React Native(Expo Go)로 구동되며, Spring Boot Public REST API(App 전용 13개)만 직접 호출한다.
얼굴 인증·장치 제어는 Kiosk 전담이며 App은 관여하지 않는다.

## Tech Stack
- Frontend: React Native 0.86 + Expo SDK 57(Expo Go) + TypeScript
- State: Zustand
- 서버 상태(TanStack Query 계열)·폼(React Hook Form 계열)·검증(Zod 계열)은 후보 단계 —
  팀이 확정하기 전까지 임의로 도입하지 않는다.

## Commands
- install: npm install
- dev(QR로 폰에서 확인): npm start
- Android 에뮬레이터: npm run android
- iOS 시뮬레이터(맥 전용): npm run ios
- 웹 미리보기: npm run web

## Code Style
- TypeScript 사용, any 타입 지양
- 화면 컴포넌트는 Screen ID(SCR-APP-AUTH-001 등) 기준으로 src/screens에 폴더 분리
- API 호출은 src/api에 작성, 실제 호출 없이 Mock 데이터로 우선 개발
- 상태 코드/enum은 src/types에 문자열 상수로 중앙 관리 (임의로 새 상태값 만들지 않음)
- 공통 컴포넌트(버튼, 배지, 로딩, 오류 배너)는 src/components에 분리

## Domain-Specific Security Rules (이 프로젝트 특수 규칙)
- App 로그인 평문 비밀번호는 제출 직후 화면 상태에서 제거한다. 로그·분석 이벤트·영속 저장소에
  절대 남기지 않는다.
- `password_hash`, `accessToken`, Toss `paymentKey`를 화면 상태·로그·캐시·분석 이벤트에 저장하지 않는다.
- 얼굴 이미지·임베딩·유사도는 App에서 다루지 않는다(Kiosk/Orin 전용 영역).
- Kiosk 얼굴 인증 세션과 App 로그인 세션은 완전히 분리한다 — 상호 재사용하지 않는다.
- `.env` 파일은 수정하지 않는다.
- 새로운 라이브러리는 추가 전 먼저 이유를 설명하고 승인받는다.

## Workflow Rules
- 구현 전에 반드시 관련 문서(PROJECT_GUIDE.md, app-screen-flow.md 해당 절)를 읽는다.
- 코드를 작성하거나 파일을 만들기 전, 어떤 파일을 왜 수정/생성할지 먼저 설명하고
  사용자의 명시적 승인을 받은 뒤에만 실제 변경을 진행한다.
- 구현 전에 먼저 `docs/plans/YYYY-MM-DD-{Screen ID}.md` 형식으로 PLAN 문서를 작성하고 승인을 받는다.
- 한 번에 하나의 Phase(Screen ID)만 구현한다.
- 구현 후 (Expo 실행 환경에서) 수동 화면 확인을 실행한다.
- 변경 사항은 PROGRESS.md에 기록한다.
- `DECISION_REQUIRED`로 표시된 항목(지도 SDK, 날씨 공급자, 대여 연장 등)은 임의로 확정하거나
  추측으로 구현하지 않는다. 반드시 "미확정"으로 표시하고 진행한다.
- 기존 기능을 수정해야 하면 먼저 이유를 설명하고 승인을 받는다.
- 문서(노션 원본)와 코드/이전 발언이 충돌하면, 12-R(API 계약) → 06번(FE 역할) → 09번(화면흐름)
  순으로 우선하되, 충돌 사실을 출처와 함께 먼저 알린다.

## Git Rules
- 하나의 commit에는 하나의 Phase(Screen ID) 변경만 포함한다.
- 빌드가 실패한 상태로 commit하지 않는다.
- 관련 없는 파일은 commit에 포함하지 않는다.
- git commit은 사용자 승인 후에만 실행한다. 사용자 지시 없이 임의로 commit/push하지 않는다.
