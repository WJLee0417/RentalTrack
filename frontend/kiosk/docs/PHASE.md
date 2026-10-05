이 문서는 현재 구현해야 하는 기능 범위를 정의한다.

AI는 반드시 현재 Phase만 구현하며, 다음 Phase의 기능을 미리 구현하거나 추측하지 않는다.

## Phase 1
### SCR-KSK-COMMON

목표

- 키오스크 공통 UI 구축

구현 범위

- KioskLayout
- 공통 Button
- Loading 화면
- Error 화면
- Success 화면
- Face Guide Overlay
- 메인 화면 UI
- 공통 색상 및 Typography
- 공통 컴포넌트 구성

완료 후

- develop 병합
- PROGRESS.md 업데이트

상태

✅ DONE (2026-07-28, develop 미병합 — 팀 확인 후 현재 브랜치에서 Phase 2로 바로 진행하기로 결정)

---

## Phase 2
### SCR-KSK-RENTAL

목표

사용자는 키오스크에서 우산을 대여할 수 있다.

구현 범위

- 대여 시작 화면
- 얼굴 인증 화면
- 얼굴 인증 API(Mock)
- 인증 성공/실패
- 대여 진행 화면
- 슬롯 안내
- 대여 완료 화면
- 메인 화면 복귀
- 예외 처리

> ⚠️ 참고(2026-07-28): `CameraCaptureScreen.tsx`, `RecognitionGuideScreen.tsx`, `SlotGuideScreen.tsx`, `App.tsx`의 카메라 스트림 배선(커밋 `549dd35`, `d3c962a`)이 Phase 2 정식 착수 승인 전에 이미 이 브랜치에 커밋되어 있다. PLAN 문서 없이 만들어진 재사용 가능한 UI 껍데기일 뿐, 실제 대여 흐름 로직(얼굴 인증 API 연동, 세션 상태 머신, 화면 전환)과는 연결돼 있지 않다. 코드는 되돌리지 않고 이 기록으로만 남긴다 — Phase 2 정식 착수 시 이 파일들을 그대로 재사용할지, 새로 만들지는 그때 PLAN 문서에서 다시 판단한다. 상세 배경은 `PROGRESS.md` "Phase 범위 초과 커밋" 절 참고.

> ⚠️ 보류 항목(2026-07-30, 사용자 확인): 출처 미확인 신규 화면흐름 표(`docs/09-screen-flow.md`와 Variant·판정값 체계가 다른 표, 노션 버전/일자 미확인)와 비교했을 때 Phase 2 범위에서 다음 2개 미구현 갭이 발견됨. 구현 여부 자체가 미정이며, 우선순위를 낮춰 보류하기로 결정 — 새 표의 출처가 확인되고 `docs/09-screen-flow.md` 갱신 여부가 정해진 뒤 재논의한다.
> - `RETURN_DISABLED_NO_CAPACITY`: 반납 가능 슬롯 0일 때 반납 버튼 비활성화 (`kioskMainStore.ts`의 `returnableSlotCount`는 현재 표시 전용, `MainScreen.tsx`의 반납 `ActionCard`엔 `disabled` 미연결)
> - `FACE_ALIGNMENT_REQUIRED`: 얼굴 검출 후 정렬 불량 시 동적 재안내 상태 (`types/faceAuth.ts`의 `AUTH_SCREEN_VARIANT`에 없음, PROJECT_GUIDE.md §17상 오류가 아닌 정상 안내 상태라 Mock 설계 자체도 미정)

상태

✅ DONE (2026-07-30 — 위 2개 보류 항목 제외 완료. TEMP TEST-ONLY로 임시 우회했던 Pi WebSocket/Orin 스트림 연동 코드는 실제 구현으로 원복 완료, 상세는 PROGRESS.md 참고)

---

## Phase 3
### SCR-KSK-RETURN

목표

사용자는 키오스크에서 우산을 반납할 수 있다.

구현 범위

- 반납 시작 화면
- 얼굴 인증 화면
- 얼굴 인증 API(Mock)
- 반납 진행 화면
- 슬롯 안내
- AI 검수 결과 화면
- 완료 화면
- 예외 처리

AI 결과 정책

NORMAL
→ 반납 완료

DAMAGED
→ 관리자가 확인 중

UNCERTAIN
→ 관리자가 확인 중

FAILED
→ 관리자가 확인 중

상태

✅ CURRENT PHASE (2026-07-30 착수)

---

## Phase 4
### SCR-KSK-ADMIN

목표

관리자는 키오스크를 관리할 수 있다.

구현 범위

- 관리자 비밀번호 입력
- 관리자 인증(Mock)
- 슬롯(우산함) 상태 조회
- 슬롯 제어
- 팬 ON/OFF
- 관리자 종료

상태

⏸ WAITING

---

# Rules

- 현재 CURRENT PHASE만 구현한다.
- 다음 Phase는 절대 구현하지 않는다.
- Mock API만 사용한다.
- DECISION_REQUIRED 항목은 구현하지 않는다.
- 화면 ID는 09-screen-flow.md 기준으로 사용한다.
- 완료 후 PROGRESS.md를 갱신한다.
- 완료 후 다음 Phase로 CURRENT PHASE를 변경한다.