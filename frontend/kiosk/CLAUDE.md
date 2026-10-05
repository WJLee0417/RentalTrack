# Billisan Kiosk - Project Rules

## Must Read Before Coding
- Read @docs/PROJECT_GUIDE.md before implementation (팀 프로젝트 지침서)
- Read the relevant SCR-KSK-* section in @docs/09-screen-flow.md for the current phase
- Read @docs/PHASE.md and implement only the current phase
- If a required doc is missing or empty, stop and ask — do not guess or proceed without it

## Project Overview
빌리산(Billisan) — 얼굴 온디바이스 인증 기반 공유 우산 대여 서비스의 키오스크(현장 무인 대여기) 프론트엔드.
React Web/PWA로 노트북 터치스크린에서 구동되며, Raspberry Pi Local API를 통해
대여/반납/얼굴인증/AI 파손검수 상태를 화면으로 표현한다.

## Tech Stack
- Frontend: React 19 + Vite + TypeScript
- Styling: Tailwind CSS v4 (@tailwindcss/vite)
- State: Zustand
- Date: dayjs
- Formatter: Prettier + prettier-plugin-tailwindcss

## Commands
- install: npm install
- dev: npm run dev
- build: npm run build
- lint: npm run lint

## Code Style
- TypeScript 사용, any 타입 지양
- 화면 컴포넌트는 Screen ID(SCR-KSK-MAIN-001 등) 기준으로 src/screens에 폴더 분리
- API 호출은 src/api에 작성, 실제 호출 없이 Mock 데이터로 우선 개발
- 상태 코드/enum은 src/types에 문자열 상수로 중앙 관리 (임의로 새 상태값 만들지 않음)
- 공통 컴포넌트(버튼, 배지, 로딩, 오류 배너)는 src/components에 분리

## Domain-Specific Security Rules (이 프로젝트 특수 규칙)
- 얼굴 이미지·영상·임베딩을 state, localStorage, sessionStorage, console.log, 오류 로그에
  절대 남기지 않는다.
- 카메라 프리뷰 프레임을 캡처해서 저장하지 않는다.
- 사용자 표시 정보(이름 등)는 세션 종료 시 즉시 초기화한다.
- .env 파일은 수정하지 않는다.
- 새로운 라이브러리는 추가 전 먼저 이유를 설명하고 승인받는다.

## Workflow Rules
- 구현 전에 반드시 관련 문서(PROJECT_GUIDE.md, 09번 화면흐름 문서)를 읽는다.
- 코드를 작성하거나 파일을 만들기 전, 어떤 파일을 왜 수정/생성할지 먼저 설명하고
  사용자의 명시적 승인을 받은 뒤에만 실제 변경을 진행한다.
- 구현 전에 먼저 `docs/plans/YYYY-MM-DD-{Screen ID}.md` 형식으로 PLAN 문서를 작성하고 승인을 받는다. (이력 보존을 위해 매 Phase마다 새 파일로 남기고 덮어쓰지 않는다.)
- 한 번에 하나의 Phase(Screen ID)만 구현한다.
- 구현 후 npm run build와 수동 화면 확인을 실행한다.
- 변경 사항은 PROGRESS.md에 기록한다.
- DECISION_REQUIRED로 표시된 항목(예: 얼굴 재시도 횟수, 실시간 갱신 방식)은
  임의로 확정하거나 추측으로 구현하지 않는다. 반드시 "미확정"으로 표시하고 진행한다.
- 기존 기능을 수정해야 하면 먼저 이유를 설명하고 승인을 받는다.
- 문서(노션 원본)와 코드/이전 발언이 충돌하면, 최신 확정일 기준 문서를 우선하되
  충돌 사실을 출처와 함께 먼저 알린다.

## Git Rules
- 하나의 commit에는 하나의 Phase(Screen ID) 변경만 포함한다.
- 빌드가 실패한 상태로 commit하지 않는다.
- 관련 없는 파일은 commit에 포함하지 않는다.
- git commit은 사용자 승인 후에만 실행한다. 사용자 지시 없이 임의로 commit/push하지 않는다.