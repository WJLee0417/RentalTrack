# 공개 데모 배포

공개 주소: <https://wjlee0417.github.io/RentalTrack-Demo/>

## 배포 구조

현재 비공개 저장소의 요금제에서는 Pages를 제공하지 않아, 공개 배포 전용 [RentalTrack-Demo](https://github.com/WJLee0417/RentalTrack-Demo) 저장소를 사용한다. 기존 저장소의 가시성은 유지한다.

1. `refactor/rentaltrack-web-skeleton`의 프론트 변경이 push되면 `deploy-demo.yml` 실행.
2. `npm ci` → `npm run build:demo` → Chromium에서 `npm run check:demo`.
3. 검증한 `dist/`만 artifact로 전달하고, 전용 SSH deploy key로 공개 저장소의 `site/`에 게시.
4. 공개 저장소의 `Deploy public demo` Actions가 Pages artifact를 배포.

원본 저장소의 PR에서는 빌드·검사까지만 실행한다. 공개 저장소에는 정적 HTML/JS/CSS·글꼴·샘플 도식과 배포 워크플로를 보관한다. 백엔드 소스·DB·실제 자료·환경 파일은 배포하지 않는다. 소스 맵 생성도 활성화하지 않는다.

## 데모 범위

- `.env.demo`는 공개 플래그 `VITE_DEMO_MODE=true`만 포함한다.
- `npm run build:demo`는 `/RentalTrack-Demo/` base 경로로 생성한다.
- `npm run build`와 평소 개발 모드는 기존 Spring API를 사용한다.
- 데모는 브라우저 안의 고정 샘플을 조회한다. API 서버 호출·실제 로그인·회원가입·데이터 쓰기는 없다.
- 역할 선택은 화면을 둘러보기 위한 데모 기능이며 서버 인증이 아니다. 새로고침하면 일반회원 화면으로 돌아간다.
- 샘플 도식의 SHA-256은 저장소 파일 바이트에서 계산했다. `check-demo-assets.mjs`로 검증한다.

## 배포 권한

원본 저장소의 Actions secret `RENTALTRACK_DEMO_DEPLOY_KEY`에는 공개 배포 저장소 한 곳에만 쓰기 권한이 있는 새 SSH 키를 저장한다. 사용자 개인 GitHub 인증 토큰을 복사하지 않는다. 공개 저장소의 Deploy keys에 `RentalTrack demo Actions publisher`로 등록했다. 키 교체 시 해당 Deploy key와 Actions secret을 함께 갱신한다.

## 로컬 확인

```powershell
cd frontend/web
npm ci
npm run build:demo
npx playwright install chromium
npm run check:demo
npm run preview -- --mode demo
```

브라우저에서 `http://localhost:4173/RentalTrack-Demo/` 접속. 설치된 Chrome으로 검사할 경우 `$env:PLAYWRIGHT_CHANNEL='chrome'`을 지정할 수 있다.

정적 배포 검사는 데스크톱·모바일에서 실제 base 경로, 사진 로딩, 역할 전환, 차수 열람, 입력 제한, API 호출 없음, 브라우저 오류와 가로 넘침을 확인한다. 원본 서버의 인증·DB·대여/검수 쓰기 검증과 별도다.
