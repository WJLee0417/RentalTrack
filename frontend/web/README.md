# 빌리산 관리자 웹 (`frontend/web`)

얼굴인증 기반 우산 대여/반납 서비스 **빌리산**의 관리자 콘솔 프론트엔드입니다.

## 기술 스택

| 구분 | 사용 |
| --- | --- |
| 언어 | TypeScript |
| 프레임워크 | React 18 |
| 빌드 | Vite 5 |
| 라우팅 | React Router 6 |
| 스타일 | Tailwind CSS 3 + shadcn/ui |
| 전역 상태 | Zustand |
| 서버 상태 | TanStack Query v5 |
| HTTP | Axios (공통 응답 규격 언랩 인터셉터) |
| 폼/검증 | React Hook Form + Zod |
| 품질 | ESLint + Prettier |

> 스택 근거: Notion `기술 스택` / `팀 컨벤션` 문서. 프레임워크는 팀 논의로 **Vite + React Router** 확정.

## 사전 준비 (⚠️ 이 PC엔 아직 Node가 없음)

1. **Node.js 20 LTS** 설치 → https://nodejs.org (또는 nvm-windows)
2. **pnpm** 활성화 (Node 설치 후 1회):
    ```bash
    corepack enable
    ```

## 시작하기

```bash
# frontend/web 디렉터리에서
pnpm install
cp .env.example .env.development   # 이미 있으면 생략
pnpm dev
```

- 개발 서버: http://localhost:5173
- `/api/*` 요청은 `vite.config.ts` 프록시로 `http://localhost:8080`(백엔드) 로 전달됩니다.
  백엔드 주소가 다르면 프록시 target 또는 `.env`의 `VITE_API_BASE_URL`을 수정하세요.

## 스크립트

| 명령 | 설명 |
| --- | --- |
| `pnpm dev` | 개발 서버 |
| `pnpm build` | 타입체크 + 프로덕션 빌드 |
| `pnpm preview` | 빌드 결과 미리보기 |
| `pnpm lint` / `pnpm lint:fix` | ESLint |
| `pnpm format` / `pnpm format:check` | Prettier |
| `pnpm typecheck` | 타입 검사만 |

## 폴더 구조 (Notion 팀 컨벤션 기준)

```
src/
├── app/            # 라우팅·프로바이더·전역 레이아웃 wiring (router, AppProviders, ProtectedRoute)
├── assets/         # 폰트, 아이콘 등 정적 자산
├── components/
│   ├── layout/     # AppShell, AppHeader, AppSidebar
│   └── ui/         # shadcn/ui 기반 순수 UI (button, input, card, label)
├── config/         # env 등 앱 설정
├── features/       # 도메인 단위 (api / components / hooks / stores / types / pages)
│   ├── auth/       # 로그인·인증 (샘플 완성)
│   └── dashboard/  # 대시보드 (뼈대)
├── lib/            # axios, queryClient, utils 등 기술 유틸
├── shared/         # 여러 기능 공통 (constants / hooks / types)
└── styles/         # 전역 스타일 + 디자인 토큰
```

## 컨벤션 (팀 표준 반영됨)

- **코드 스타일**: 들여쓰기 4칸, 세미콜론 O, 싱글쿼트 — `.prettierrc.json` / `.editorconfig`에 설정됨
- **경로 alias**: `@/` → `src/` (예: `import { cn } from '@/lib/utils'`)
- **커밋**: `{이슈키} {type}: 내용` (예: `S15P11A201-50 feat: 로그인 화면 구현`)
- **브랜치**: `develop`에서 분기 → `{이슈키}-fe-{영문설명}` (예: `S15P11A201-50-fe-login-page`)
- **API 응답**: 백엔드 공통 규격(`statusCode/timestamp/path/message/data/error`)을 axios 인터셉터가 `data`만 언랩. 타입은 `src/shared/types/api.ts`.

## shadcn/ui 컴포넌트 추가

Node 설치 후:

```bash
pnpm dlx shadcn@latest add dialog table dropdown-menu ...
```

설정은 `components.json`에 이미 되어 있습니다 (style: new-york, baseColor: slate).

## 다음 작업 힌트

1. 백엔드 로그인 API 명세 확정 → `src/features/auth/api/authApi.ts`, `types/index.ts` 필드 맞추기
2. 도메인 화면 추가: `features/<domain>/pages` 생성 → `shared/constants/navigation.ts`에 메뉴 등록 → `app/router.tsx`에 라우트 연결
3. 목록 화면은 shadcn `table` + TanStack Query 조합 권장
