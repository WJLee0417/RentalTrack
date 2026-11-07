# 공개 데모 배포

공개 주소: <https://wjlee0417.github.io/RentalTrack/>

## 배포 구조

`refactor/rentaltrack-web-skeleton` 브랜치에서 GitHub Actions로 빌드하고, 같은 RentalTrack 저장소의 GitHub Pages에 직접 배포한다.

1. 해당 브랜치의 프론트 변경이 push되면 `deploy-demo.yml` 실행.
2. `npm ci` → `npm run build:demo` → Chromium에서 `npm run check:demo`.
3. 검증한 `dist/`를 Pages artifact로 전달하고 `github-pages` 환경에 배포.

PR에서는 빌드·검사까지만 실행한다. 배포 브랜치 push와 해당 브랜치의 수동 실행에서만 게시한다. 별도 배포 저장소·SSH 키·사용자 인증 토큰은 필요하지 않다. `pages: write`, `id-token: write` 권한은 배포 job에만 부여한다.

## 데모 범위

- `.env.demo`는 공개 플래그 `VITE_DEMO_MODE=true`만 포함한다.
- `npm run build:demo`는 `/RentalTrack/` base 경로로 생성한다.
- `npm run build`와 평소 개발 모드는 기존 Spring API를 사용한다.
- 데모는 브라우저 안의 고정 샘플을 조회한다. API 서버 호출·실제 로그인·회원가입·데이터 쓰기는 없다.
- 역할 선택은 화면을 둘러보기 위한 데모 기능이며 서버 인증이 아니다. 새로고침하면 일반회원 화면으로 돌아간다.
- 샘플 도식의 SHA-256은 저장소 파일 바이트에서 계산했다. `check-demo-assets.mjs`로 검증한다.
- Pages artifact에는 정적 HTML/JS/CSS·글꼴·샘플 도식만 포함한다. 백엔드·DB·환경 파일은 배포하지 않는다.

## 로컬 확인

```powershell
cd frontend/web
npm ci
npm run build:demo
npx playwright install chromium
npm run check:demo
npm run preview -- --mode demo
```

브라우저에서 `http://localhost:4173/RentalTrack/` 접속. 설치된 Chrome으로 검사할 경우 `$env:PLAYWRIGHT_CHANNEL='chrome'`을 지정할 수 있다.

정적 배포 검사는 데스크톱·모바일에서 실제 base 경로, 사진 로딩, 역할 전환, 차수 열람, 입력 제한, API 호출 없음, 브라우저 오류와 가로 넘침을 확인한다. 원본 서버의 인증·DB·대여/검수 쓰기 검증과 별도다.
