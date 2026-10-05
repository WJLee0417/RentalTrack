# Billisan Kiosk

얼굴 온디바이스 인증 기반 공유 우산 대여 서비스의 키오스크(현장 무인 대여기) 프론트엔드

## 기술 스택

- **Framework**: React 19 + Vite + TypeScript
- **Styling**: Tailwind CSS v4 (`@tailwindcss/vite`)
- **State**: Zustand
- **Date**: dayjs
- **PWA**: vite-plugin-pwa
- **Formatter/Lint**: Prettier(+ prettier-plugin-tailwindcss), ESLint

## 시작하기

```bash
npm install
npm run dev
```

`.env` 파일이 필요하다 (직접 만들지 말고 팀 채널에서 값 받을 것 — 임의로 수정 금지):

```
VITE_FACE_STREAM_TOKEN=
VITE_PI_WS_RENT_URL=
VITE_PI_WS_RETURN_URL=
```

## 스크립트

| 명령어 | 설명 |
|---|---|
| `npm run dev` | 개발 서버 실행 |
| `npm run build` | 타입 체크(`tsc -b`) 후 프로덕션 빌드 |
| `npm run lint` | ESLint 실행 |
| `npm run preview` | 빌드 결과 로컬 미리보기 |

## 폴더 구조

```
src/
├── api/          # Pi WebSocket 통신, 각 화면별 API 클라이언트
├── assets/       # 이미지·아이콘 등 정적 리소스
├── components/
│   ├── common/   # 공통 컴포넌트 (버튼, 배지, 로딩 등)
│   ├── icons/    # 아이콘 컴포넌트
│   └── layout/   # 공통 레이아웃
├── screens/      # Screen ID(SCR-KSK-*) 기준 화면 폴더
│   ├── SCR-KSK-MAIN-001/
│   ├── SCR-KSK-AUTH-001/
│   ├── SCR-KSK-RENT-001/
│   ├── SCR-KSK-RETURN-001/
│   └── common/
├── store/        # Zustand 스토어
└── types/        # 상태 코드·오류 코드 등 타입/상수
```

## 참고 문서

- [`CLAUDE.md`](./CLAUDE.md) — 프로젝트 규칙, 코드 스타일, 보안 규칙
- [`docs/PROJECT_GUIDE.md`](./docs/PROJECT_GUIDE.md) — 기획 문서 기준 전체 지침서 (통신 경계, API 계약, 정책 등)
- [`docs/09-screen-flow.md`](./docs/09-screen-flow.md) — 화면별 상세 흐름·API 스키마
- [`docs/PHASE.md`](./docs/PHASE.md) — 현재 구현 Phase
