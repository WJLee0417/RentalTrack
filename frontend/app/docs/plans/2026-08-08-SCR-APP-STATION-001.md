# PLAN: SCR-APP-STATION-001 재구현 (카카오맵 JS SDK 커스텀 타일셋)

## 0. 배경 및 결정 번복

- Phase 7(SCR-APP-STATION-001)은 2026-08-05 `DEC-SF-004`(RESOLVED: 실제 지도 SDK 없이 SVG 가상
  캠퍼스 지도)를 기준으로 이미 구현 완료(✅ DONE)된 상태였다.
- 2026-08-08 대화에서 사용자가 카카오맵 JS SDK의 커스텀 타일셋(`kakao.maps.Tileset`) 샘플 코드를
  제시하며 이 방식으로 Phase 7을 다시 진행하기로 결정 — `DEC-SF-004`를 **공식 번복**한다
  (PROJECT_GUIDE.md §8/§13, PHASE.md Phase 7 갱신 필요, 아래 §7 참고).
- 번복 사유: 실제 카카오맵 엔진(핀치 확대 시 focal point 유지, 관성 스크롤 등)을 그대로 활용해
  기존 커스텀 제스처 구현(`useCampusMapGesture.ts`)의 한계(중앙 고정 확대, 관성 없음 — 이전
  대화에서 확인)를 해소하기 위함.

## 1. 확정된 결정 사항 (2026-08-08 대화 기준, § 표시는 실사용 테스트로 검증 완료)

| 항목 | 결정 |
|---|---|
| 구현 방식 | WebView(`react-native-webview`) + 카카오맵 JS SDK + 커스텀 Tileset |
| 카카오 JS API 키 | 발급 완료, `.env`의 `EXPO_PUBLIC_KAKAO_JS_KEY`로 사용 중 ✅ |
| 타일 크기 | 512×512px (카카오 공식 샘플과 동일 규칙) |
| 줌 레벨 범위 | ✅ (2026-08-09 확정, 5장→14장으로 확대) 0단계(1536×1536, 3×3=9장, 최대 확대) +
  1단계(1024×1024, 2×2=4장) + 2단계(512×512, 1장, 최소 확대/전체 지도) = **총 14장**. `limit =
  Math.ceil(3 / 2^z)` — 카카오 공식 샘플과 동일한 공식 |
| 타일 제작 방식 | 사용자가 디자인 툴에서 직접 수작업 export (자동 분할 스크립트 불필요) |
| **HTML 호스팅** | ✅ **불필요로 결론.** `source={{ html: htmlString }}`로 RN에서 생성한 HTML 문자열을 직접 WebView에 주입 — Expo Go 실기기 테스트에서 카카오 SDK 도메인/Referer 검증이 이 방식에서 통과함을 확인(2026-08-08). Netlify/Surge 배포, EC2, `kakao-map-web/` 별도 정적 프로젝트, 카카오 콘솔 `localhost` 도메인 등록 **전부 불필요** — §0(배경)의 "외부 호스팅" 전제를 폐기 |
| **타일 이미지** | base64 data URI로 HTML 문자열 안에 직접 임베드(§4). 5장 스코프에선 유효, 장수가 크게 늘면 재검토 필요 |
| 마커·상세 정보 UX | ✅ 마커는 WebView(카카오 엔진) 안에서 렌더링, 탭 시 `postMessage`로 RN에 알려 **기존 네이티브 `StationBottomSheet` 재사용** — 실기기 확인 완료 |

## 2. 남은 것 (✅ 2026-08-09 전부 해결)

- [x] ~~카카오 콘솔 `localhost` 등록~~ — 불필요 확인됨(§1)
- [x] ~~Netlify/Surge 배포~~ — 불필요 확인됨(§1)
- [x] `.env`에 `EXPO_PUBLIC_KAKAO_JS_KEY` 추가 — 완료
- [x] **실제 타일 이미지 14장** — 사용자가 디자인 툴에서 직접 export, `src/assets/campus-tiles/`에
  보관 + `kakaoTiles.generated.ts`에 base64로 임베드 완료
- [x] 좌표계(§3), fallback 불투명화, 드래그 제한, 파일 x/y 배정 — 전부 실기기로 최종 확인 완료
- [ ] 다음 단계는 §4의 기존 파일 삭제 + PROJECT_GUIDE.md/PHASE.md/PROGRESS.md 문서 갱신

## 3. 좌표계 설계 (✅ 2026-08-09 실기기 확인으로 최종 확정)

- 기존 `Station.x/y`(0~1 비율, SVG viewBox 912×1300 기준)는 계약 그대로 유지 — `stationApi.ts`,
  `types/station.ts` 변경 없음.
- **핵심 발견(2026-08-09)**: `projectionId: null`일 때 카카오 좌표계는 **left-bottom이 (0,0)**이고
  y가 위로 갈수록 증가한다(공식 문서 확인). 처음엔 이미지 좌표계(y=0이 위, y가 아래로 증가) 그대로
  음수(`-y`)를 넣는 실수를 했고, 이게 지도 중심이 실제 타일이 없는 좌표로 어긋나 카카오 실제 지도가
  비쳐 보이는 문제(§7)의 근본 원인이었다. 최종 변환: 마커 좌표 `y = (1 - station.y) * WORLD_SIZE`
  (이미지 y=0(위)→Coords y=WORLD_SIZE(위), 이미지 y=1(아래)→Coords y=0(아래)), 지도 중심
  `Coords(WORLD_SIZE/2, WORLD_SIZE/2)`(양수).
- 0단계(가장 세밀한 레벨, 3×3) 기준 전체 지도 픽셀 크기 `WORLD_SIZE = 1536`(512×512 타일 3×3).
- 카카오 공식 샘플의 `y = -y - 1` 타일 좌표 뒤집힘은 `getTile(x, y, z)` 콜백 내부에서 그대로 사용 —
  라벨 붙은 디버그 플레이스홀더 14장으로 격자 배치 자체가 정상임을 실기기로 확인(2026-08-09).
- 범위 밖 요청 시 **불투명** fallback 타일 필요 — 최초엔 투명 PNG를 썼다가 카카오 기본 실제 지도가
  그대로 비쳐 보이는 문제가 있어 옅은 회색 불투명 타일(`TILE_BLANK_FALLBACK`)로 교체(2026-08-09).
- 드래그(팬)는 **허용**(`map.setDraggable(true)`, 2026-08-09 최종 확인) — 14장 범위를 벗어나면
  옅은 회색 빈 화면이 보일 수 있음을 감수하기로 함(자유롭게 이동 가능한 UX를 우선).
- **실제 타일 파일의 x/y 배정 실수**: 사용자가 처음 만든 14장 중 일부(`0_0_1`↔`0_1_0` 등)는 실제
  이미지 내용과 파일명의 x/y가 전치(transpose)되어 있었음 — 좌표 코드와 무관한 별개 문제였고,
  사용자가 파일명을 좌표순으로 재배정해 해결(2026-08-09).

## 4. 파일 변경 목록

### 신규 (RN 앱) — ✅ 구현 완료(2026-08-08), 플레이스홀더 타일로 동작 확인됨
- `src/screens/SCR-APP-STATION-001/KakaoMapWebView.tsx` — 기존 `CampusMap.tsx` 대체. `stations`로
  HTML 문자열을 생성(`buildHtml`)해 `source={{ html }}`로 WebView에 직접 주입(외부 호스팅 없음,
  §1). 카카오 SDK 로드 + `Tileset.add`(getTile로 base64 타일 반환) + 마커 렌더 + `postMessage`
  브릿지가 전부 이 파일 안에 포함. `onMessage`로 `STATION_SELECTED` 수신해 `onSelectStation` 호출
- `src/screens/SCR-APP-STATION-001/kakaoTiles.generated.ts` — 타일 5장 + 투명 fallback 1장을
  base64 data URI 상수로 보관. 지금은 Python(PIL) 스크립트로 만든 단색+라벨 플레이스홀더. 실제
  타일 준비되면 같은 이름의 상수 6개만 재생성해서 교체(수작업 재타이핑 금지 — base64는 스크립트로
  생성/복사할 것, 손으로 옮기다 깨진 전례 있음)

### 수정
- `src/screens/SCR-APP-STATION-001/MapScreen.tsx` — `CampusMap` → `KakaoMapWebView` 교체(그 외
  로직 동일 — `stationApi`/`StationBottomSheet` 변경 없음)
- `package.json` — `react-native-webview` 의존성 추가 (Expo Go 호환 확인 완료, `npx expo install
  react-native-webview`로 설치)

### 삭제 (더 이상 쓰이지 않음 — 백업 없이 완전 삭제)
- `src/screens/SCR-APP-STATION-001/CampusMap.tsx`
- `src/screens/SCR-APP-STATION-001/hooks/useCampusMapGesture.ts` (폴더까지)
- `src/screens/SCR-APP-STATION-001/mapGeometry.ts`
- `src/screens/SCR-APP-STATION-001/StationMarker.tsx` (마커는 이제 WebView 안 카카오 엔진이 렌더링)

### 유지 (변경 없음)
- `src/types/station.ts`, `src/api/stationApi.ts`, `src/screens/SCR-APP-STATION-001/StationBottomSheet.tsx`
- `src/assets/campus-map.svg` — 삭제하지 않음. 타일 이미지 export 시 원본 소스로 재사용 가능하므로
  보존(단, RN 코드에서의 import는 제거)

### 문서
- `PROJECT_GUIDE.md` §8 `DEC-SF-004` 행 갱신(RESOLVED: SVG 가상 지도 → RESOLVED: 카카오맵 JS SDK
  커스텀 타일셋, 번복 사유·일자 기록), §13 변경 이력에 2026-08-08 항목 추가
- `PHASE.md` Phase 7 상태 갱신(✅ DONE → 재오픈 사유·새 구현 방식 기록)
- `PROGRESS.md` 갱신(작업 완료 후)

## 5. 새 라이브러리 승인 요청

- `react-native-webview` — Expo SDK 54(package.json 기준 실제 버전, AGENTS.md가 가리키는 v57 문서와
  불일치 확인됨) 공식 문서 확인 결과 Expo Go에서 별도 dev client 없이 바로 동작. **이 PLAN 승인에
  설치 승인도 포함되는 것으로 진행해도 될까요?**

## 6. 구현 순서 (갱신됨 — 외부 호스팅 단계 전부 삭제)

1. ~~`react-native-webview` 설치~~ ✅ 완료
2. ~~`kakao-map-web/index.html` 골격 작성~~ ✅ `KakaoMapWebView.tsx`가 이 역할을 대신함(인라인 HTML)
3. ~~로컬 브라우저 확인~~ ✅ 스킵 — Expo Go 실기기에서 바로 검증(더 빠르고 정확함)
4. ✅ `KakaoMapWebView.tsx` 작성 + `MapScreen.tsx` 연결, `postMessage` 브릿지로 `StationBottomSheet`
   연동까지 Expo Go에서 확인 완료(마커 탭 → 시트 오픈 정상, 2026-08-08 버그 수정 후)
5. **다음 단계**: 사용자가 실제 타일 이미지 5장 준비되면 `kakaoTiles.generated.ts` 교체
6. ~~Netlify/Surge 배포~~ 불필요 — 삭제
7. 기존 파일 삭제(§4의 "삭제" 목록) + 문서 갱신(PROJECT_GUIDE.md §8/§13, PHASE.md Phase 7,
   PROGRESS.md) — 실제 타일 교체 후 최종 확인까지 마치고 진행 예정

## 7. 리스크 / 확인 필요 사항 (갱신됨)

- ~~카카오 `Tileset`의 `Coords`↔`getTile(x,y,z)` 좌표 변환 규칙 불확실~~ — 실기기 테스트로 대부분
  해소(§2 참고). `y = -y - 1`이 타일·마커 모두에 일관 적용되어 중앙 정렬은 정상. 다만 이건 5장짜리
  최소 스코프에서 검증된 것이라, 타일이 늘어나는 경우(z2 이상) 재검증 필요.
- `EXPO_PUBLIC_` 접두사 환경변수는 클라이언트 번들에 그대로 노출됨 — JS 키는 도메인 화이트리스트로
  보호되는 값이라 이 프로젝트의 보안 규칙(CLAUDE.md "Domain-Specific Security Rules")에서 말하는
  `accessToken`/`paymentKey`류의 민감정보와는 성격이 다르다고 판단해 진행하나, 이견 있으면 알려달라.
- 카카오 SDK 도메인/Referer 검증이 `source={{ html }}` 방식에서 통과하는 건 **문서화된 공식 동작이
  아니라 실기기 경험적 확인**(2026-08-08, iOS 기준)이라, Android에서 다르게 동작하거나 카카오 측
  정책 변경 시 깨질 수 있음 — 문제 생기면 그때 외부 호스팅으로 되돌리는 걸 대안으로 남겨둔다.
