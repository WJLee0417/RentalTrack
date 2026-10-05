# PLAN — SCR-APP-MYPAGE-001 (연장하기·신고하기 확인 모달)

> 대상 Phase: PHASE.md "Phase 4" 재작업(마이페이지, 이미 DONE 처리된 항목에 모달 추가)
> 근거: 사용자가 공유한 실제 와이어프레임 스크린샷(Extend modal / Frame 13, 2026-08-04) — 신규 저장 안 됨,
> 이 PLAN 문서에 구성만 기록
> 문서 상태: `DEC-SF-006`(연장)·`DEC-SF-007`(신고)는 노션 문서 기준 여전히 `DEFERRED_NOT_CONTRACTED`(API
> 계약 없음). 사용자 확인(2026-08-04): **UI 모달만 구현, Mock 처리** — 실제 연장/신고가 서버에 반영되지
> 않는다는 점을 유지한다.

---

## 1. 구현 목표

- 마이페이지 현재 대여 카드의 "연장하기"/"신고하기" 버튼(현재 no-op)을 눌렀을 때, 와이어프레임과 동일한
  확인 모달을 띄운다.
- 모달의 확인 버튼(연장하기/신고하기)을 눌러도 **실제 서버 상태는 변하지 않는다** — `DEFERRED_NOT_CONTRACTED`
  범위이므로 Mock으로 모달만 닫는다(성공/실패 API 없음). 취소 버튼과 동작 차이는 UI 표시뿐이다.
- API·타입·Mock 함수를 새로 만들지 않는다(만들 12-R 계약이 없음).

## 2. 만들 파일 목록

| 구분 | 경로 | 역할 |
|---|---|---|
| 신규 | `src/components/common/ConfirmModal.tsx` | 공용 확인 모달(X 닫기, 제목/설명, 선택적 경고 배너, 취소+확인 버튼) |
| 수정 | `src/screens/SCR-APP-MYPAGE-001/MyPageScreen.tsx` | 모달 2개 상태 추가, 연장/신고 버튼에 연결 |

`ConfirmModal`을 공용 컴포넌트로 두는 이유: 두 모달이 구조(X 닫기 아이콘, 제목, 설명, 취소/확인 버튼 2개)가
동일하고 색상·경고 배너 유무만 다르다 — 각 화면에 중복 작성하지 않는다.

## 3. `ConfirmModal` 설계

```ts
interface ConfirmModalProps {
  visible: boolean
  title: string
  description: string
  warningText?: string        // 신고 모달의 분홍 경고 배너(있을 때만 렌더)
  cancelLabel?: string         // 기본 "취소"
  confirmLabel: string
  confirmVariant?: 'primary' | 'danger'  // 기본 primary(노랑), 신고는 danger(error-cta 빨강)
  onCancel: () => void
  onConfirm: () => void
}
```

- 레이아웃: 반투명 배경(`bg-black/40`) 중앙에 흰색 카드(rounded-2xl), 우상단 X 아이콘(`close`,
  `MaterialCommunityIcons`) → `onCancel`
- `warningText` 있으면 `bg-error-bg`/`text-error-text` 배너로 표시(신고 모달 전용)
- 취소 버튼: outline(`border border-disabled`), 확인 버튼: `confirmVariant`에 따라
  `bg-primary`(연장) 또는 `bg-error-cta`(신고) — 기존 `Button` 컴포넌트는 이 두 변형을 지원하지 않아
  `ConfirmModal` 내부에서 직접 `Pressable`로 그린다(SelectField와 동일한 기존 패턴)

## 4. `MyPageScreen` 변경

- `extendModalVisible`/`reportModalVisible` 상태(useState) 추가
- 기존 no-op `Pressable`(연장하기/신고하기)의 `onPress`를 각각 모달 열기로 변경
- 연장 모달: title "대여 기간을 연장할까요?", description "1회 연장이 가능합니다.", confirmLabel "연장하기",
  confirmVariant "primary"
- 신고 모달: title "우산 파손·분실을 신고할까요?", description "신고 후 관리자 확인을 거쳐 처리돼요.",
  warningText "분실·파손 시 우산가액이 청구될 수 있어요.", confirmLabel "신고하기", confirmVariant "danger"
- `onConfirm`은 두 모달 다 모달을 닫기만 함(Mock — 실제 반영 없음). 주석으로 `DEFERRED_NOT_CONTRACTED`,
  API 연동 안 됨을 명시.

## 5. 테스트 방법

1. 대여 중 상태에서 "연장하기" 클릭 → 모달 노출, 취소/X로 닫힘 확인
2. "연장하기" 확인 버튼 클릭 → 모달만 닫히고 화면 상태(대여 상태·기한) 변화 없음 확인(Mock이므로 정상)
3. "신고하기" 클릭 → 경고 배너 포함 모달 노출, 빨간 확인 버튼 확인
4. `npx tsc --noEmit` 통과 확인

## 6. 이번에 하지 않는 일

- 실제 연장/신고 API 호출·서버 반영 — `DEC-SF-006`/`DEC-SF-007` 미확정 상태 유지
- 연장 가능 횟수·조건의 실제 검증(문구는 와이어프레임 그대로 고정 텍스트)
