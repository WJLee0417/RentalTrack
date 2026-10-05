# 빌리산(Billisan) 프로젝트 지침서

> 기준: 빌리산 기획문서 **v3.0** (2026-07-29 전환, `BREAKING_CONTRACT_AND_ARCHITECTURE_CHANGE`) — 이번 갱신은 **06번 "빌리산 프론트엔드 역할 명세서 v3.0", 09번 "Kiosk·App 화면 흐름 명세 v3.0", 08번 "Billisan ERD 및 데이터 아키텍처 문서 v3.0"을 원문 전체 확인**(각 95,734자·130,123자·138,145자, 06·09번 2026-07-30 / 08번 2026-07-30 추가 확인) 후 반영했고, 여기에 **12-I "Billisan 장치 메시지·내부 인터페이스 명세서 v3.0"의 Kiosk↔Pi WebSocket 7개 Operation 개별 페이지 및 상위 공통 문서**("P0 구현 기준", 75,727자, 2026-07-30 추가 확인)까지 반영해 하나로 병합한 최종본이다. 00·01~05·07·10~12(12-R 전체, 12-I 중 Spring-Pi MQTT 10개·Pi-Orin WebSocket 부분)는 이번 작업 범위에서 원문을 확인하지 않았다.
> v3.0 문서군 전체 상태는 `DOCUMENT_STATUS=REVIEW_REQUIRED · RELEASE_STATUS=NOT_RELEASED · CHAIN_SYNC_STATUS=IN_PROGRESS · CORRECTION_STATUS=IN_PROGRESS`다. **v2.0 때와 달리 아직 "팀 확인 후 기준선 채택" 절차가 이루어지지 않았다** — 이 문서는 팀 채택 여부와 무관하게 06번·09번·08번·12-I(부분) 원문 확인 결과를 정확히 반영하기 위한 갱신이며, v3.0을 프로젝트의 새 기준선으로 선언하는 것은 아니다. 단, 06번 문서 자체의 헤더는 `CORRECTION_STATUS=APPLIED`로 표기되어 있어 문서군 전체 플래그(`IN_PROGRESS`)와 다르다 — 이 불일치는 팀 재확인 필요. 08번 문서 헤더도 `CORRECTION_STATUS=APPLIED · SECONDARY_AUDIT_REQUIRED=true`로 06번과 유사한 패턴을 보인다.
> 작성 대상: Frontend 1 (Hyejin)
> 최종 갱신: 2026-07-30

---

## 빠른 참조

- **v3.0 최대 변경점(breaking)**: Kiosk↔Pi 통신이 다시 **WebSocket**으로 전환 확정됐다(06번 §1·§4.1, 2026-07-29자 변경이력 "v3.0 Kiosk Frontend WebSocket Client 전환"). v2.0에서 "Kiosk FE → Spring Kiosk API(`/api/v1/kiosk`) 7개 HTTP 직접 호출"이라고 했던 것은 **폐기**됐다. 기존 `KSK-*` 7개 ID는 그대로 유지되지만 이제 **Pi WebSocket의 논리 Operation ID**이지 HTTP Method/Path가 아니다.
- **로컬 `DEC-032`가 예외가 아니라 규칙이 됐다**: v2.0판 지침서는 "얼굴 인증 트리거('준비되었습니다' 클릭) 1곳만 Pi 직접 WebSocket 허용, 나머지는 Spring REST"라고 좁게 취급했었다. 06번 v3.0 원문에 따르면 **7개 오퍼레이션 전부**가 Pi WebSocket이다. 즉 `DEC-032`는 팀이 임의로 낸 국지적 예외가 아니라 이후 공식 문서가 채택한 방향과 같은 방향이었던 것으로 보인다 — 다만 이는 06번 원문 확인 결과이지, `DEC-032`를 낸 시점(2026-07-29)에 이 정보를 알고 결정한 것인지는 확인 불가.
- **Kiosk FE가 실제로 사용하는 것은 Pi WebSocket 오퍼레이션 7개뿐**(12-I 계약 권위, 엔드포인트 `ws[s]://{piHost}:{kioskWsPort}/ws/v1/kiosk`): `KSK-STATION-001`(대여소 상태), `KSK-SESSION-001`(세션 열기), `KSK-AUTH-001`(얼굴 인증), `KSK-SESSION-002`(세션 상태 조회), `KSK-RENT-001`(대여), `KSK-RETURN-001`(반납), `KSK-CANCEL-001`(세션 취소). Spring·MQTT·Orin·GPIO 직접 접근은 전부 금지.
- **ACK ≠ 최종 성공**: 모든 오퍼레이션은 요청 → ACK → 최종 Result/Terminal 3단계로 구분된다. ACK 수신만으로 완료·100% 표시 금지, 반드시 최종 Result 수신 + Spring/MySQL Commit 확인 후에만 완료 화면 전환(06번 §4.1·§4.2·§7).
- **문서 체계 개편**: 10번은 "화면·인터페이스 추적용 비권위 문서"로, 12번은 "계약 인덱스(요약)"로 각각 격하됐다(사용자 제공 배경 정보 기준, 06번 원문에서 10/12번 격하 자체를 직접 언급하지는 않음 — 재확인 필요). 상세 권위는 신설된 **12-R**(REST API 계약 DB, App/Admin 23개, 06번 §6에서 mention-page로 링크 확인)과 **12-I**(장치·내부 인터페이스 계약 DB — Kiosk WS 7 + Spring-Pi MQTT 10 + Pi-Orin WS, 06번 §6·§8에서 mention-page로 링크 확인)로 분리됐다. **이번에 12-I 중 Kiosk↔Pi WebSocket 7개 Operation 개별 페이지 + 상위 공통 문서는 전량 확인 완료했다**(Heartbeat·공통 Envelope·`messageType` 명명 규칙 등, 3.1·5.4.1절 참고). 12-R 본문과 12-I의 Spring-Pi MQTT 10개·Pi-Orin WS 부분은 여전히 읽지 않았다.
- **Kiosk WS 7개 Operation 메시지 스키마 확인 완료(신규, 12-I 원문 확인, 2026-07-30)**: Heartbeat는 WebSocket Ping/Pong **10초 간격**, **30초 무응답 시 해당 연결만 `KIOSK_WEBSOCKET_UNAVAILABLE`** 판정 후 재연결한다. 공통 Message Envelope(`schemaVersion,messageType,messageId,operationId,sessionId,requestId,correlationId,sentAt,expiresAt,sequence,payload`), 공통 ACK(`ackStatus,acceptedAt,errorCode`, Timeout 2초 공통), 공통 Error(`KIOSK.OPERATION.ERROR`, Payload `code,messageCode,retryable,details`)도 확정됐다. `messageType`은 `KIOSK.<도메인>.<REQUEST|ACK|EVENT|RESULT>` 패턴을 따른다. **단, 재연결 최대 횟수·지수 Backoff 계수·Jitter 범위는 12-I 본문 전체를 확인해도 없어 여전히 미확인**이다 — 임의로 수치를 하드코딩하지 않는다(상세는 3.1·5.4.1·9.2·11·13.4절 참고).
- **키오스크 폼팩터 확정**: "터치 입력 지원 노트북 화면"으로 확정됐다(06번 §2.6, 및 2026-07-28자 변경이력 "Kiosk 화면 장비 기준 교정" — 기존 7인치 터치 LCD 표기를 노트북 화면으로 교정). v2.0판 지침서가 "재확인 필요"로 남겨뒀던 항목이 해소됨.
- **정산 규칙**: 무료 24h → 연체 1일 1,000원 → 7일 후 분실(7,000원) / 파손 7,000원 / 1건당 최대 7,000원(비합산) — 이번 06번 확인으로도 이 수치 자체에 대한 변경 근거는 나오지 않았다. 04번 정책 문서 v3.0 원문은 여전히 미확인.
- **신설 결정 권위 4개** (08번 ERD 원문 전체 확인 완료, 2026-07-30 — 아래는 08번 원문 기준으로 전부 "확인됨"으로 갱신):
  - `DEC-047`(P1-8 새 ERD 권위선) — **08번 §변경이력에서 확인됨**: "P1-7 Docker 통과 결과를 활성 스키마로 채택, MySQL Table 11개, `ADMIN_ACCOUNT` 분리, `USER_ACCOUNT.role` 제거, `DEVICE_OPERATION` Option A(`rental_id`·`return_attempt_id` 제거) 확정, `STATION.station_code`·`location_text`는 P0 필수 아님". FE 활성 계약(App/Kiosk API 개수)에는 영향 없음.
  - `DEC-048`(신원 모델: 9자리 학번 PK + 내부 가명 UUID) — **08번 원문에서 확인됨**. 08번 §변경이력 "`DEC-048 · V13 학번 PK·가명 user_ref 기준선`": `USER_ACCOUNT.user_id CHAR(9)`가 정확히 9자리 숫자 학번 PK, `USER_ACCOUNT.user_ref CHAR(36)`가 Spring 생성 UUID(별도 UNIQUE 컬럼, PK 아님). Orin `FACE_PROFILE`의 PK는 `user_ref UUID` 자체(학번은 Orin에 전달하지 않음). 상세는 3.2·3.3절 참고.
  - `DEC-049`(통신 경계 권위) — 06번 §8·08번 §1.0에서 모두 "기준 결정: `DEC-049`"로 명시적으로 확인됨. Kiosk↔Pi WebSocket 전환의 근거 결정이며, 08번은 이를 "Kiosk↔Pi·Pi↔Orin WebSocket 아키텍처와 Kiosk 7개 계약 권위 이전만 결정"한다고 범위를 한정한다(스키마 자체의 근거는 아님).
  - `DEC-050`(데이터·무결성 권위) — **08번 원문에서 확인됨**. 08번 §1.0·§11.0·§19.5: 활성 물리 스키마 권위는 `DEC-050 V18_SCHEMA_INTEGRITY_BASELINE`이며, 첨부 SQL(`billisan_backend3_p0_p1_final_realfinal_schema.sql`)이 물리 권위다. MySQL 업무 SSOT 11개 + 내부 Trigger Guard 1개 = 물리 12개, Trigger 9개, Orin PostgreSQL 업무 1개, Pi 영속 DB 0개. **단, "V1~V18" 각 마이그레이션 파일이 개별적으로 무엇을 하는지 한 줄씩 나열한 목록은 08번 본문에 없다** — "V1~V18 clean-install SQL"은 첨부 SQL 파일 전체를 가리키는 집합적 표현이며, 이 문서에서 실제로 버전 번호가 명시된 것은 `DEC-048=V13`, `DEC-050=V18`(최종) 두 지점뿐이다. 상세는 12.1·13절 참고.
- **DECISION_REQUIRED 전체 목록**: 9장 참고. v2.0판의 `DEC-KSK-001~007`류 항목 중 다수가 06번·09번 v3.0에서도 여전히 미확정으로 남아있다(세션 상태 전달 방식, 팬 제어, 관리자 모드 등).
- **아직 원문을 안 읽은 문서(v3.0 기준)**: 00, 01, 02, 03, 04, 05, 07, 10, 11, 12, 12-R — 06·09·08번은 전문 확인했고, **12-I는 Kiosk↔Pi WebSocket 7개 Operation 개별 페이지 + 상위 공통 문서만 확인했다**(Spring-Pi MQTT 10개·Pi-Orin WebSocket 부분, 12-R 본문은 여전히 미확인).
- **P0 계약 40개 재구성**: `HTTP_REST_COUNT=23`(App 13 + Admin 10, Frontend가 직접 호출하는 HTTP Endpoint) + `KIOSK_WEBSOCKET_OPERATION=7`(HTTP 아님, Pi WebSocket) + `SPRING_PI_MQTT_OPERATION=10`(Frontend 직접 호출 안 함) = `P0_LOGICAL_CONTRACT_COUNT=40`(`CONFIRMED=38 · PROVISIONAL=2`). v2.0판의 "Kiosk Spring 7"이라는 표현은 "Kiosk WebSocket Operation 7"로 정정해야 한다.

---

## Claude 작업 지침 (이 프로젝트 전용)

1. 화면/코드 구현을 요청받으면 **항상 구조 파악 → 계획·리스크 정리 → 승인 대기 → 구현** 순서를 지킨다. 승인 전에는 파일을 만들거나 수정하지 않는다.
2. 노션 문서를 인용할 때는 몇 번 문서(00~12, 12-R, 12-I)의 어느 절인지 출처를 밝힌다.
3. `DECISION_REQUIRED`·`PROVISIONAL_API`·`DEC-XXX` 항목은 임의로 확정하거나 추측으로 구현하지 않는다. 반드시 "미확정"으로 표시하고 진행한다.
4. 문서 간 내용이 충돌하면, v3.0 기준으로는 **12-I(장치·내부 인터페이스 계약, Kiosk WS 7 포함) → 12-R(REST API 계약, App/Admin) → 06번(FE 역할) → 09번(화면흐름) → 12번(계약 인덱스 요약) → 그 외** 순으로 우선하되(단 12-R은 원문 미확인이므로 실제 우선순위는 재확인 후 확정), 충돌 사실 자체를 반드시 먼저 알린다.
5. 이 지침서 자체를 갱신할 때는 맨 아래 "변경 이력"에 갱신 사유를 남긴다.
6. 아부성 표현·의미 없는 이모티콘 금지, 답변 전 팩트체크 2회 이상, 애매한 부분은 근거 부족을 명시, 꼬리질문 3개 선제시 — 사용자 전역 설정과 동일하게 이 프로젝트에서도 적용.

---

## 0. 문서 정보

| 항목 | 내용 |
|---|---|
| 서비스명 | Billisan (빌리산) — 얼굴 온디바이스 인증 기반 공유 우산 대여 서비스 |
| 소속 | SSAFY 15기 A201팀, AIoT 공통 프로젝트 |
| 개발 기간 | 2026-07-24 ~ 2026-08-07 (11 Working Day) |
| 문서 버전 | **v3.0** (`REVIEW_REQUIRED · NOT_RELEASED`, 06·09·08번 원문 확인 + 12-I Kiosk WS 부분 확인, 팀 채택 여부 미확정) |
| 참고 노션 | 06번: `https://app.notion.com/p/5c5a5037c20c82c58172011ff3961651` · 09번: `https://app.notion.com/p/736a5037c20c824ba430014fd569c825` · 08번: `https://app.notion.com/p/ad5a5037c20c827ea09581bb4406f430` |

**주의**: v2.0(2026-07-27~28 전환) 시점에는 팀이 원문 확인 후 로컬 기준선으로 채택했지만, 이번 v3.0은 06·09·08번 전문 확인 + 12-I 부분 확인 상태다. 아래 표의 "원문 확인 여부"는 이 갱신 기준으로 다시 표시한다.

| 번호 | 문서명 | 상태 | 원문 확인 여부(v3.0 기준) |
|---|---|---|---|
| 00 | 통합 변경 이력 | `REVIEW_REQUIRED`(v2.0판 상태, v3.0 갱신 여부 미확인) | ❌ 미확인 (v2.0판만 확인함) |
| 01 | PRD v3.0(06번 §0.0 기준문서 목록에 이름만 등장) | 미확인 | ❌ 미확인 |
| 02 | 요구사항 명세서 v3.0(06번 §0.0 기준문서) | 미확인 | ❌ 미확인 |
| 03 | 유저 스토리·인수 조건 | 미확인 | ❌ 미확인 |
| 04 | 정책 명세서 v3.0(06번 §0.0 기준문서) | 미확인 | ❌ 미확인 (정산 값 자체는 이번에도 변경 근거 없음) |
| 05 | 백엔드 역할 명세서 | 미확인 | ❌ 미확인 |
| 06 | **프론트엔드 역할 명세서 v3.0** | `API_BASELINE_SYNCED · REVIEW_REQUIRED · NOT_RELEASED`(문서 자체는 `CORRECTION_STATUS=APPLIED`) | ✅ **전문 확인**(95,734자, 2026-07-30) |
| 07 | 임베디드 역할 명세서 | 미확인 | ❌ 미확인 |
| 08 | **ERD·데이터 아키텍처 v3.0** | `TRANSPORT_DATA_FLOW_SYNCED · REVIEW_REQUIRED · NOT_RELEASED · CORRECTION_STATUS=APPLIED · SECONDARY_AUDIT_REQUIRED=true` | ✅ **전문 확인**(138,145자, 2026-07-30). `DEC-048`(신원모델)·`DEC-050`(데이터·무결성) 근거 문서임을 확인, 3.2·3.3·12.1·13절에 반영 |
| 09 | **Kiosk·App 화면 흐름 명세 v3.0** | `REVIEW_REQUIRED · NOT_RELEASED` | ✅ **전문 확인**(130,123자, 2026-07-30, Kiosk 부분은 `docs/09-screen-flow.md`에 반영 완료) |
| 10 | Kiosk·App 공통 도메인 API 명세 | 사용자 배경 정보상 "화면·인터페이스 추적용 비권위 문서"로 격하 | ❌ 미확인 (격하 사실 자체도 06·09번 원문에서 직접 확인 못함) |
| 11 | 관리자 Web 화면 흐름 명세 | 미확인 | ❌ 미확인 (Hyejin 담당 아님) |
| 12 | 통합 API 명세 | 사용자 배경 정보상 "계약 인덱스(요약)"로 격하 | ❌ 미확인 |
| 12-R | **REST API 계약 DB(신설, App/Admin 23개)** | 06번 §6에서 mention-page로 권위 문서 인용 확인 | ⚠️ 06번의 인용 문구만 확인, 본문 미확인 |
| 12-I | **장치·내부 인터페이스 계약 DB(신설, Kiosk WS 7 + Spring-Pi MQTT 10 + Pi-Orin WS)** | Kiosk WS 7개 Operation 개별 페이지 전체 + 상위 공통 문서("P0 구현 기준", 75,727자) 전량 확인(2026-07-30) | ✅ **Kiosk WS 7개 메시지 스키마·Heartbeat(10초/30초)·공통 Envelope 확인 완료**. ⚠️ 재연결 최대 횟수·지수 Backoff 계수·Jitter 범위는 12-I 본문에도 없어 여전히 미확인. Spring-Pi MQTT 10개·Pi-Orin WS 부분은 이번에 참고 수준으로만 훑음(본격 확인 대상 아님) |

---

## 1. 서비스 범위 (변경 없음, 06·09번 v3.0에서도 동일하게 확인)

- 실제 대여소 1개, 독립 슬롯 3~5개 기준(실제 시연은 물리 SLOT 1개, 06번 §1.1)
- 사용자는 얼굴 인증 후 슬롯 기반으로 우산을 대여·반납
- **대여 전 파손 AI 검수는 수행하지 않음** (`AVAILABLE` = 정상 가정)
- **모든 반납**에서 Jetson이 우산 전체를 표준 구도로 촬영하고 AI 보조 검수 수행 (손상 부위 별도 촬영 없음)
- AI 결과는 공식 파손 상태·폐기·과금을 자동 확정하지 않음
- 관리자가 반납 슬롯의 품질 상태를 최종 판정
- 사용자 대상 예약 기능 없음 — `RESERVED` 상태 UI 만들지 않음

---

## 2. 팀 구성 및 역할

| 역할 | 인원 | 담당 플랫폼 |
|---|---|---|
| Frontend 1 (Hyejin) | 1명 전담 | React Native 사용자 앱, 현장 키오스크 (React Web/PWA) |
| Frontend 2 | PM 겸임 | React 관리자 웹 |
| Backend | 3명 | Spring Boot / MySQL |
| Embedded | 1명 | Jetson Orin Nano, Raspberry Pi 5 |

**Frontend 1 핵심 책임**: 사용자 로그인·현황·미정산/결제·챗봇·마이페이지, 키오스크 전체 상태 머신(Pi WebSocket 세션 → 얼굴 인증 → 대여/반납 → AI 검수 → 슬롯 안내 → 복구/연결 끊김 화면까지 포함, 06번 §1)

> ⚠️ 지도가 App P0 범위에서 빠져있다(5.3절 참고, v2.0부터 유지). 06번 v3.0 §3.1의 App P0 API 13개 목록에도 `STATION-001/002`는 없다 — 이번 확인으로도 변경 없음.

**주 담당 원칙**
- 모바일 앱·키오스크 사용자 화면 주 담당: Frontend 1
- 관리자 웹 화면 주 담당: Frontend 2
- 공통 TypeScript DTO·상태 코드·오류 코드·디자인 토큰은 공동 관리, 렌더링 컴포넌트는 분리
- 지도: 모바일 지도 = Frontend 1(P1), 관리자 분포도 = Frontend 2
- 장치 잠금·팬 제어는 프론트엔드 소유 기능이 아님 — 명령 요청과 처리 상태만 표현

---

## 3. 시스템 책임 경계 (v3.0 — **재차 아키텍처 변경, breaking**)

| 구성요소 | v3.0 책임 | 금지 사항 |
|---|---|---|
| **Jetson Orin Nano** | 얼굴 검출·임베딩·매칭, YOLOv8n 우산 탐지, PatchCore 파손 판정, Pi↔Orin 로컬 WebSocket으로만 통신 | Kiosk FE와 직접 연결, Spring 직접 호출, 원본 이미지 영속 저장·외부 공개, 얼굴 임베딩 외부 반환 |
| **Raspberry Pi 5** | **Kiosk FE의 유일한 직접 상대** — Kiosk WebSocket 서버 역할(7개 Operation 처리), Spring과는 MQTT로 업무 요청·응답(10개 Operation)·물리 Command·Terminal·Health·Snapshot 교환, Jetson과는 로컬 WebSocket, 센서·잠금·LED·팬 제어, 장치 상태 머신 | 업무 데이터의 영속 저장, 얼굴 매칭, AI 모델·런타임 |
| **Spring Boot** | MySQL 업무 SSOT, App/Admin 인증, App Public REST 13개 + Admin REST 10개 제공(총 23개, 12-R 권위), Pi와는 MQTT로만 통신, 대여·반납·검수·정산·결제·관리자 판정 | Kiosk FE와 직접 통신(HTTP/WebSocket 불문), 얼굴 이미지 영속 저장 |
| **App·Admin Web** | Spring Boot REST API(App 13 / Admin 10) 직접 호출 | Pi·Orin·MQTT 직접 접근 |
| **Kiosk FE** | **Raspberry Pi WebSocket Client** — `KSK-*` 7개 Operation만 사용(12-I 권위), ACK와 최종 Result/Terminal을 구분해 표현, 연결 수명주기(Heartbeat·Timeout·지수 Backoff/Jitter 재연결) 관리 | **Spring Public·Admin REST, MQTT, Orin WebSocket, DB, GPIO·잠금·센서·팬에 직접 접근 절대 금지** — Pi가 모든 내부 통신을 중계 |

> ⚠️ **breaking change 이력 정리 (06번 §변경이력 기준)**:
> - v1.0(07-24): Kiosk FE → Raspberry Pi Local API(`/local-api/v1`, REST)
> - 07-27~07-28(`DEC-024`, v2.0): Kiosk FE → **Spring Kiosk API**(`/api/v1/kiosk`, REST) 7개로 전환. Pi는 Kiosk FE에서 보이지 않는 존재로 재정의.
> - 07-29(프로젝트 로컬 `DEC-032`): 얼굴 인증 트리거 1곳에 한해 Pi 직접 WebSocket 연결 예외 허용 — `DEC-024` 원칙의 국지적 번복.
> - **07-29(06번 v3.0, "Kiosk Frontend WebSocket Client 전환")**: Kiosk FE → **Raspberry Pi WebSocket**로 전 오퍼레이션 재전환. Pi Local REST Client 완전 제거. 기존 7개 `KSK-*` ID는 유지하되 WebSocket 논리 Operation으로 재정의.
> - 07-30(`DEC-049` 정리): 위 WebSocket 전환을 활성 기준으로 재확인·정리.
> - **결론**: 로컬 `DEC-032`는 "예외"가 아니라, 이후 공식 문서가 채택한 것과 같은 방향(Kiosk↔Pi WebSocket)이었다. 다만 `DEC-032`는 7개 중 얼굴 인증 트리거 1곳만 언급했던 반면, 06번 v3.0은 **7개 전부**를 WebSocket Operation으로 규정한다 — 적용 범위가 다르므로 `DEC-032`의 좁은 서술을 그대로 유지해서는 안 되고, 9.1절처럼 갱신이 필요하다.

### 3.1 Kiosk WebSocket 연결·메시지 수명주기 (06번 §1.1·§2.2.1·§4.3, 09번 §1·§3, 12-I "P0 구현 기준" 공통 문서 K-0~K-3절, 2026-07-30 12-I 원문 확인 반영)

- Kiosk Runtime당 단일 `KioskWebSocketClient`를 생성하고 Pi Endpoint(`ws[s]://{piHost}:{kioskWsPort}/ws/v1/kiosk`)에만 연결한다. 인증 완료 전 업무 Operation 송신 금지. 연결 수립 후 Kiosk가 `KIOSK.CONNECT`를 보내면 Pi가 `KIOSK.CONNECTED`와 `connectionId(UUID)`로 응답한다(12-I K-0절, 신규 확인).
- **Heartbeat·Timeout 수치 확정(12-I K-0절, 2026-07-30 확인)**: WebSocket Ping/Pong을 **10초 간격**으로 사용하고, **30초 동안 유효 Pong이 없으면 해당 연결만 `KIOSK_WEBSOCKET_UNAVAILABLE`로 판정**하고 재연결한다. 동일 Kiosk 인스턴스의 중복 연결은 새 `connectionId`가 기존 연결을 승계하며 이전 연결의 메시지는 거절된다.
- **여전히 미확인**: 최대 재연결 횟수, 지수 Backoff 계수, Jitter 범위 — 12-I 공통 문서(75,727자 전량 확인)와 7개 Operation 개별 페이지 어디에도 이 수치가 없다. "재연결은 지수 Backoff와 Jitter를 적용한다"는 것은 06번 문서의 원칙적 서술일 뿐 계수·범위 값 자체는 12-I 원문에도 없으므로 임의로 하드코딩하지 않는다. 진행 중 명령은 재연결 시 자동 재발행하지 않는다.
- **공통 Message Envelope(12-I K-1절, 신규 확인)**: 모든 Request/ACK/Event/Result/Error가 `schemaVersion(String,"1.0"), messageType(String), messageId(UUID), operationId(Enum, KSK-* 7개 중 하나), sessionId(UUID|null), requestId(UUID), correlationId(UUID), sentAt(DateTime), expiresAt(DateTime), sequence(Long), payload(Object)`를 공유한다. `sessionId`는 Station 조회·세션 생성 Request에서만 `null`이다.
- **공통 ACK/Terminal/Error(12-I K-2절, 신규 확인)**: ACK Payload는 `ackStatus(ACCEPTED|DUPLICATE|REJECTED), acceptedAt, errorCode|null`이며 모든 Operation ACK Timeout은 **2초**로 통일돼 있다. 최종 Result는 `terminalStatus(SUCCEEDED|FAILED|CANCELLED|RECOVERY_REQUIRED)`를 포함한다. 공통 Error `messageType=KIOSK.OPERATION.ERROR`, Payload `code, messageCode, retryable, details|null`(원문 예외·Stack Trace 미포함).
- `requestId`·`correlationId`·`sessionId`로 요청·ACK·최종 Result/Event를 상관시킨다. **ACK는 수신 확인일 뿐 업무 성공이 아니다.** 재연결·ACK 유실 재전송은 같은 `(sessionId,operationId,requestId)`에 새 `correlationId`만 바꿔 사용한다(12-I K-2절).
- **Operation별 `messageType` 명명 규칙(12-I 개별 페이지 7개 전체 확인)**: `KIOSK.<도메인>.<REQUEST|ACK|EVENT|RESULT>` 패턴 — `KIOSK.STATION_SUMMARY.*`(KSK-STATION-001), `KIOSK.SESSION.OPEN.*`(KSK-SESSION-001), `KIOSK.FACE_AUTH.*`(KSK-AUTH-001), `KIOSK.SESSION.STATE.*`(KSK-SESSION-002), `KIOSK.RENT.*`(KSK-RENT-001), `KIOSK.RETURN.*`(KSK-RETURN-001), `KIOSK.SESSION.CANCEL.*`(KSK-CANCEL-001). Error는 7개 전부 `KIOSK.OPERATION.ERROR`로 공통.
- Route 재진입·React Strict Mode·재렌더링으로 동일 Listener가 중복 등록되지 않도록 Handler Registry를 Operation/Session 단위로 관리한다.
- 화면 이동·세션 완료·취소·로그아웃·Unmount에서는 해당 Subscription과 Handler를 해제한다.
- 연결이 끊기면 조작을 잠그고 `KIOSK_WEBSOCKET_UNAVAILABLE` 안내와 재연결 상태를 표시한다. 로컬 캐시·마지막 ACK로 완료를 추정하지 않는다.
- 재연결 후에는 보유한 `sessionId`로 `KSK-SESSION-002` Snapshot을 재조회해 서버/Pi 최종 상태와 조정한 뒤 화면을 복구한다.
- 세션 생성 요청은 `mode, stationId, clientSessionKey, lastKnownSessionId, operationRequestIds{faceAuthRequestId, rentalRequestId, returnRequestId}`를 유지한다. `clientSessionKey`는 Web Crypto로 생성해 **sessionStorage에만** 두고 사용자 정보·학번·`userRef`를 포함하지 않는다.
- `MQTT_BROKER_UNAVAILABLE`(Spring↔Pi 내부, FE가 직접 겪는 오류는 아니지만 원인 구분용), `KIOSK_WEBSOCKET_UNAVAILABLE`, `ORIN_WEBSOCKET_UNAVAILABLE`(Pi↔Orin 내부)을 서로 다른 사용자 상태·운영 로그로 표현한다.
- 제품은 3~5 SLOT을 표현할 수 있지만 P0 실제 시연은 1 SLOT이다. 화면은 응답 배열 길이에 따라 렌더하고 가짜 슬롯·고정 선택 UI를 만들지 않는다.
- 09번 v3.0 §0.0의 연결 상태 Enum(`CONNECTING → CONNECTED → REQUEST_ACCEPTED → PROCESSING → COMPLETED|FAILED`, 단절 시 `RECONNECTING`)이 모든 Kiosk 화면에 공통 적용된다 — 상세는 `docs/09-screen-flow.md` §3.1·§5.0 참고.

### 3.2 얼굴 등록 이미지 흐름 (v2.0 `DEC-022`·`DEC-023`, 06번 v3.0에서 문언 변경 없이 확인, 08번 ERD로 상세 확인)

- 얼굴 원본·임베딩은 브라우저·앱·키오스크의 파일·캐시·상태 저장소·오류 수집·로그에 영속 저장하지 않는다(06번 §2.3).
- Kiosk에는 이름·학번·`loginId`·UUID·`userRef`를 표시하거나 Local 상태/sessionStorage/log에 저장하지 않는다.
- 얼굴 등록·재등록 원본 예외는 내부 프로비저닝이며 FE 일반 업무 상태에 남기지 않는다(06번 §2.3, v2.0 `DEC-022`/`DEC-023` 원칙과 동일 문구는 아니나 취지 일치).
- **08번 ERD 원문 확인 결과(2026-07-30)**: v2.0판 지침서 §3.2의 `FACE_PROFILE.user_ref` PK 구조는 **08번 원문에서 확인됨**. Orin PostgreSQL `FACE_PROFILE` 테이블의 PK는 `user_ref UUID` 자체이며(08번 §7.3: "권위 기준: `user_ref UUID`가 PK다. `face_profile_id`, `registration_status`는 사용하지 않으며 행 존재 여부가 얼굴 등록 여부의 최종 권위다"), 학번(`user_id`)은 Orin에 전달·저장하지 않는다. MySQL `USER_ACCOUNT.user_ref CHAR(36) UNIQUE`와는 물리 FK가 아닌 UUID 논리 참조로만 연결된다.
- **얼굴 등록 이미지 흐름 상세(08번 §1.1·§2.2·§2.6.3 확인)**: 얼굴 등록·재등록 원본은 Orin 단일 Camera Broker가 로컬 Capture Lease로 취득하고 Vector 생성 직후 즉시 삭제한다. Spring→Pi(MQTT)→Pi WebSocket→Orin 경로로 작업을 전달하며, 네트워크·Payload에는 UUID(`userRef`)를 직접 담지 않고 `profileTicket`(등록·재등록·삭제 60초)·`faceMatchToken`(인증 Challenge 30초)·`faceMatchEvidence`(Orin→Spring 결과 30초)·`authGrant`(후속 대여·반납 120초) 같은 별도 Token/Evidence를 사용한다(08번 §2.3·§19 변경이력 `FACE_TOKEN_BOUNDARY_CONFIRMED`, 2026-07-30). 이 Token/Evidence 자체도 업무 테이블·로그에 영속하지 않는다.

### 3.3 인증·계정 모델 (v2.0 `DEC-031` 확정 사항, 06번 v3.0에서도 재확인, 08번 ERD로 `DEC-048` 확인)

- App 로그인은 대학 계정 형식의 `login_id`와 사용자가 입력한 평문 비밀번호를 HTTPS로 Spring에 전송한다. 클라이언트가 해시를 생성하거나 DB 해시 문자열을 입력받지 않는다(06번 §2.2·§3.1 API 표).
- `password`, `password_hash`는 App 응답·타입·캐시·로그·분석 이벤트 어디에도 포함하지 않는다(06번 §2.3).
- `AUTH-001`·`AUTH-002`·`USER-001`은 06번 §3.1에서도 P0 유지.
- `DEC-048`(9자리 학번 PK + 내부 가명 UUID 신원 모델)은 **08번 ERD 원문에서 확인됨**(2026-07-30, 08번 §변경이력 "`DEC-048 · V13 학번 PK·가명 user_ref 기준선`" 및 §2.3·§7.1). 정확한 구조:
  - `USER_ACCOUNT.user_id CHAR(9)` — **PK**, 정확히 9자리 숫자 학번(`CHECK 9 DIGITS`). 별도 `student_number` 컬럼은 없다.
  - `USER_ACCOUNT.user_ref CHAR(36) ASCII_BIN` — PK가 아니라 **UNIQUE 컬럼**. Spring이 생성하는 무작위 가명 UUID이며 학번·`login_id`·이름에서 유도하지 않는다. `FACE_PROFILE_SYNC_OPERATION.user_ref`의 물리 FK 대상이자 Orin `FACE_PROFILE.user_ref UUID`(PK)의 논리 참조로 쓰인다.
  - `USER_ACCOUNT.login_id VARCHAR(254)` — 학번과 다른 정규화된 대학 이메일, UNIQUE.
  - 9자리 학번과 평문 `userRef`는 Kiosk 응답·Kiosk/Orin WebSocket Payload·얼굴 관련 MQTT·Pi/Orin 저장소·로그에 전달·기록하지 않는다(08번 §2.3).
  09번은 이 정책의 화면 적용 결과(얼굴 인증 중 학번·이름·UUID 비표시, 로딩만 노출)만 규정하며, 08번은 그 근거가 되는 데이터 구조 자체를 규정한다.

### 데이터·통신 기준 (변경 없는 부분, 08번 확인으로 상세화)
- MySQL이 공식 업무 상태의 SSOT
- Orin의 얼굴 프로필·임베딩·벡터 검색 소유 구조는 v2.0판과 동일하며, **08번 원문으로 확인됨**: Orin PostgreSQL + pgvector `FACE_PROFILE` 1개 테이블(PK `user_ref UUID`, `embedding VECTOR`, `model_version`, `template_version`, `registered_at`, `updated_at`)이 SSOT다. 이름·이메일·비밀번호·원본 이미지는 저장하지 않는다(08번 §2.2·§7.3)
- `SLOT`을 재고 단위로 사용, `UMBRELLA`·`umbrellaId`는 사용하지 않음(06번 §2.1, 09번 §1에서도 재확인)
- 인증·우산 검수의 임시 이미지는 즉시 삭제, 이미지 바이너리·Base64·경로·URL·객체 키·faceEmbedding은 일반 API/DTO에 없음(06번 §2.1·§2.3)

---

## 4. 대여·정산 정책 (04번 문서 v3.0 원문 미확인 — 기존 값 유지)

| 항목 | 값 |
|---|---|
| 무료 대여 시간 | 24시간 (`RENTAL.due_at` 기준) |
| 연체료 | 1일 1,000원 |
| 분실 전환 | 연체 7일 후 |
| 파손·분실 정산액 | 각 7,000원 |
| 대여 1건당 최대 정산액 | 7,000원 (파손+연체 합산 안 함) |
| 연체·분실 전환 처리 | Spring Boot 스케줄러가 멱등(idempotent) 처리 |
| 파손 정산 생성 시점 | 관리자 최종 판정 이후에만 |

`dueAt` 관련 사항은 06번 v3.0에서도 `RENTAL-001`이 P0 API로 유지되고 "서버 산정값만 표시, 클라이언트 재계산 금지" 원칙이 재확인된다(§3.1). 다만 `dueAt` 필드 자체의 명시적 언급은 이번 06번 확인에서는 발견하지 못했다 — v2.0판 12번 §A-2.1 근거는 여전히 미확인 상태로 남아있다.

---

## 5. Frontend 1 (Hyejin) 역할 상세

### 5.1 역할 정의 (06번 §3.1 기준 갱신)

일반 사용자가 모바일 앱에서 대여 상태·미정산 비용을 확인·처리하도록 UX를 구현. 동시에 현장 키오스크의 대여·반납 전 과정을 단계형 화면 상태로 구성하되, **Kiosk FE는 Raspberry Pi의 WebSocket Client**로서 얼굴 안내·대여·반납·복구·연결 끊김 화면을 표현한다(06번 §1). Backend/Embedded가 반환하는 인증·AI·장치 결과를 신뢰 가능한 상태 흐름으로 표현. **얼굴 매칭과 장치 제어는 직접 수행하지 않음.**

### 5.2 P0 계약 40개 재구성 (06번 §6 기준, 프레이밍 변경)

```
APP_PUBLIC_REST_COUNT           = 13  (Frontend 1, Spring 직접 호출)
ADMIN_REST_COUNT                = 10  (Frontend 2, Spring 직접 호출)
HTTP_REST_COUNT                 = 23  (App 13 + Admin 10, 12-R 권위)
KIOSK_WEBSOCKET_OPERATION_COUNT = 7   (Frontend 1, Pi WebSocket — HTTP 아님, 12-I 권위)
SPRING_PI_MQTT_OPERATION_COUNT  = 10  (Pi↔Spring 내부, Frontend 직접 호출 안 함, 12-I 권위)
P0_LOGICAL_CONTRACT_COUNT       = 40
P0_CONFIRMED_COUNT              = 38
P0_PROVISIONAL_COUNT            = 2   (AUTH-001, AUTH-002 — 로컬 계정 방식은 확정, 실제 대학 SSO 전환만 미정)
```

> ⚠️ v2.0판의 `KIOSK_SPRING_P0_API_COUNT=7`이라는 표현은 폐기한다. 7개는 Spring API가 아니라 **Pi WebSocket Operation**이다. Pi↔Orin WebSocket은 이 40개 집계 밖이다(06번 §6).

### 5.3 담당 범위 — 모바일 앱 (App Public REST 13개, 변경 없음)

| 화면 | API | 핵심 책임 |
|---|---|---|
| 로그인·로그아웃 | `AUTH-001`(`POST /api/v1/auth/login`), `AUTH-002`(`POST /api/v1/auth/logout`), `USER-001` | P0 대학 계정 가정의 `login_id + password` 로그인, 세션·만료·최소 계정 표시. 비밀번호 해시 처리·비교는 Spring 전용 |
| 홈·현재 대여·내역 | `HOME-001`, `RENTAL-001`, `RENTAL-002` | 서버 상태 표시, 금액·기한·자격을 클라이언트에서 재계산하지 않음 |
| 미정산·결제 | `SETTLEMENT-001`, `PAYMENT-001~003` | 중복 제출 차단, 결제창 복귀 후 서버 최종 상태 재조회 |
| 알림·날씨 | `ALERT-001`, `WEATHER-001` | 파생 알림·논리 예보 표시, P0 알림 읽음 서버 저장 없음 |
| 챗봇 | `CHATBOT-001` | 메시지·응답·Timeout·정적 FAQ Fallback·허용 CTA |

App 지도·대여소 목록/상세, 알림 읽음 저장, 문의, 비밀번호 재설정, 대여 연장, 분실/파손 사용자 신고는 `DEFERRED_NOT_CONTRACTED`(06번 §3.3, v2.0과 동일).

**Toss Payments 딥링크**: `successRedirectUri: "billisan://payments/success"`, `failRedirectUri: "billisan://payments/fail"` — v2.0과 동일 계약 유지로 추정(06번 v3.0 활성 본문에 명시적 재확인 없음, ARCHIVED 절에만 등장).

### 5.4 담당 범위 — 키오스크 (Pi WebSocket Operation 7개, **완전 재교체**)

| Operation | 화면 책임 | 금지 경계 |
|---|---|---|
| `KSK-STATION-001` | 세션 전 Station Summary·`observedAt`·`stale`·대여/반납 가능 여부 | 사용자 자격 판단·서버 재고 확정 금지 |
| `KSK-SESSION-001`, `KSK-SESSION-002` | 세션 생성·현재 상태 Snapshot·재연결 복구 | ACK를 최종 업무 결과로 간주하거나 과거 명령 자동 재전송 금지 |
| `KSK-AUTH-001` | Pi가 전달한 `AUTH_PROCESSING`·FaceGuidance·최종 인증 결과 표시 | Orin 직접 연결, 영상 프레임·얼굴 원본·임베딩 접근 금지 |
| `KSK-RENT-001` | Spring이 선정한 SLOT 안내와 대여 완료 요약 표시 | Kiosk의 SLOT 선택·예약·완료 판정 금지 |
| `KSK-RETURN-001` | Inspection부터 물리 반납·서비스 완료·복구 화면 | Inspection 전 반납 SLOT 확정 가정 금지 |
| `KSK-CANCEL-001` | 물리 동작 전 안전 취소와 초기화 | 물리 동작 후 임의 취소·성공 처리 금지 |

- Station Summary는 Pi가 전달하는 Spring `EDGE-SNAPSHOT-001`/`EDGE-HEALTH-001` 공식 Summary만 사용한다. stale이거나 연결 단절 시 `STATION_SUMMARY_UNAVAILABLE`이나 `KIOSK_WEBSOCKET_UNAVAILABLE`로 화면을 차단한다(06번 §4.1).
- 기존 7개 ID의 요청·응답 DTO 업무 의미는 유지되지만, **HTTP Method·Path를 호출하는 Pi Local REST Client는 제거**하고 WebSocket `messageType` 기반 Operation Client로 구현해야 한다.
- **후보(구현 계약 아님, v2.0판 유지)**: `KSK-DIAG-001`(로컬 슬롯 진단, `DEFERRED_NOT_CONTRACTED`), `KSK-FAN-001`(팬 제어, `OUT_OF_SCOPE`) — 06번 v3.0 활성 본문에는 이 두 후보 ID의 재확인 서술이 없다.

### 5.4.1 Operation별 메시지 스키마 상세 (12-I 원문 확인, 2026-07-30)

> 12-I 데이터소스의 7개 Operation 개별 페이지와 상위 공통 문서("12-I. Billisan 장치 메시지·내부 인터페이스 명세서 v3.0 — P0 구현 기준")를 전량 확인해 반영. 상세 필드는 `docs/09-screen-flow.md` 3.1~3.4절에도 화면별로 반영돼 있다.

| Operation ID | Request/ACK/Event/Result messageType | 핵심 Request 필드 | 핵심 Result 필드 | 주요 Error Code | Timeout |
|---|---|---|---|---|---|
| `KSK-STATION-001` | `KIOSK.STATION_SUMMARY.{REQUEST,ACK,RESULT}`(Event 없음) | `stationId` | `stationId,observedAt,stale,operating.status,inventory{rentableSlotCount,emptyReturnSlotCount},availability{rentAvailable,returnAvailable,blockingReasons}` | `STATION_SUMMARY_UNAVAILABLE\|STATION_MISMATCH\|DEVICE_OFFLINE\|SNAPSHOT_STALE\|COMMAND_EXPIRED` | ACK 2초·Result 3초·TTL 5초 |
| `KSK-SESSION-001` | `KIOSK.SESSION.OPEN.{REQUEST,ACK,EVENT,RESULT}` | `mode,stationId,clientSessionKey,lastKnownSessionId,operationRequestIds{faceAuthRequestId,rentalRequestId,returnRequestId}` | `sessionId,mode,status(ACTIVE\|RECOVERING\|COMPLETED\|FAILED\|CANCELLED\|RECOVERY_REQUIRED),expiresAt` | `SESSION_CONFLICT\|STATION_MISMATCH\|SESSION_RECOVERY_REQUIRED\|COMMAND_EXPIRED\|KIOSK_WEBSOCKET_UNAVAILABLE` | ACK 2초·Result 3초 |
| `KSK-AUTH-001` | `KIOSK.FACE_AUTH.{REQUEST,ACK,EVENT,RESULT}` | `authRequestId` | `sessionId,authRequestId,authenticated,eligibility,kioskStep,userMessageCode,retryable`(이름·학번·userRef 없음) | `FACE_NOT_DETECTED\|MULTIPLE_FACES\|AUTHENTICATION_FAILED\|ORIN_WEBSOCKET_UNAVAILABLE\|ORIN_RESULT_TIMEOUT\|MQTT_BROKER_UNAVAILABLE\|USER_NOT_ELIGIBLE\|COMMAND_EXPIRED` | ACK 2초·Terminal 15초 |
| `KSK-SESSION-002` | `KIOSK.SESSION.STATE.{REQUEST,ACK,RESULT}`(Event 없음) | 없음(선택 `lastSeenSequence`) | `sessionId,mode,status,kioskStep,faceGuidance,eligibility,checkoutSlot,returnSlot,rentalId,returnAttemptId,returnAttemptStatus,inspectionResult,progressMode,rentalSummary,failureCode,userActionCode,userMessageCode,retryable,expiresAt` | `SESSION_NOT_FOUND\|SESSION_EXPIRED\|SESSION_RECOVERY_REQUIRED\|COMMAND_EXPIRED` | ACK 2초·Result 3초 |
| `KSK-RENT-001` | `KIOSK.RENT.{REQUEST,ACK,EVENT,RESULT}` | `rentalRequestId` | `sessionId,rentalRequestId,terminalStatus,status,rentalId,checkoutSlotId,slotNumber,rentalSummary{rentalId,rentedAt,dueAt}` | `USER_NOT_ELIGIBLE\|SLOT_NOT_AVAILABLE\|SLOT_CONFLICT\|MQTT_BROKER_UNAVAILABLE\|DEVICE_OFFLINE\|DEVICE_COMMAND_TIMEOUT\|UNLOCK_FAILED\|PHYSICAL_REMOVAL_NOT_DETECTED\|STATE_RECONCILIATION_REQUIRED\|COMMAND_EXPIRED` | ACK 2초·제거 30초·Result 35초 |
| `KSK-RETURN-001` | `KIOSK.RETURN.{REQUEST,ACK,EVENT,RESULT}` | Envelope `requestId`(별도 Payload 필드 없음) | `sessionId,requestId,returnAttemptId,rentalId,returnSlotId,slotNumber,terminalStatus,status,inspectionResult,userMessageCode` | `ORIN_WEBSOCKET_UNAVAILABLE\|ORIN_RESULT_TIMEOUT\|INSPECTION_FAILED\|SLOT_NOT_AVAILABLE\|MQTT_BROKER_UNAVAILABLE\|SENSOR_TIMEOUT\|LOCK_FAILED\|RETURN_ALREADY_COMPLETED\|STATE_RECONCILIATION_REQUIRED\|COMMAND_EXPIRED` | ACK 2초·AI검수 30초·전체 90초 |
| `KSK-CANCEL-001` | `KIOSK.SESSION.CANCEL.{REQUEST,ACK,RESULT}`(Event 없음) | `reasonCode(USER_CANCELLED\|SCREEN_EXIT\|OPERATOR_CANCELLED)` | `sessionId,terminalStatus=CANCELLED,status=CANCELLED,cancelledAt` | `SESSION_NOT_FOUND\|SESSION_ALREADY_TERMINAL\|CANCEL_NOT_ALLOWED\|PHYSICAL_OPERATION_IN_PROGRESS\|CAMERA_CAPTURE_IN_PROGRESS\|COMMAND_EXPIRED` | ACK 2초·Result 3초 |

- 공통 Error `messageType`은 7개 전부 `KIOSK.OPERATION.ERROR`이며 Payload는 `code,messageCode,retryable,details|null`이다.
- **미확인**: `ACTIVE_RENTAL_EXISTS`/`ACTIVE_RENTAL_NOT_FOUND`/`UNSETTLED_EXISTS`(17장 오류 코드표)가 위 표 중 어느 Error Code·Result 필드에 대응하는지 12-I 원문에 명시가 없다. 추정 매핑을 확정값으로 쓰지 않는다.
- `KSK-CANCEL-001`은 취소 허용 경계(얼굴 Camera Capture Lease·Spring 물리 Command Publish·삽입/잠금/검수 시작 전까지만 허용, 이후는 `CANCEL_NOT_ALLOWED`)가 있다 — 화면 이탈만으로는 이 Operation을 호출하지 않는다(3장·9.1절 원칙과 일치).

> ⚠️ **API 공백 — 여전히 미해결**: 06·09번 v3.0에서도 "얼굴 등록" 전용 Operation이 7개 목록에 없다. `KSK-AUTH-001` 재사용 여부는 이번 확인으로도 해소되지 않았다.

**반납 화면 단계** (06번 §4.2·09번 3.4절, Enum 자체는 v2.0판과 동일 9단계, 09번 v3.0에서는 화면 Variant 명칭이 일부 정식화됨 — 상세는 `docs/09-screen-flow.md` 참고):
```
INSPECTION_GUIDE → INSPECTION_CAPTURE → INSPECTION_PROCESSING →
RETURN_SLOT_GUIDE → WAITING_FOR_UMBRELLA_INSERTION → LOCKING_RETURN_SLOT →
PHYSICAL_RETURN_DONE → RETURN_COMPLETED
```
- `progressMode=INDETERMINATE`. 로컬 capped animation(`LOCAL_SIMULATED_CAPPED`)은 최종 성공 Result 전 **0~90%까지만** 증가하고 90%에서 대기한다. ACK·중간 Event·연결 복구만으로 100%를 표시하지 않는다.
- 최종 성공 메시지 수신 + Spring/MySQL Commit 확인 후에만 100%와 완료를 짧게 표시한다.
- 실패·Timeout·연결 단절·취소·`RECOVERING|RECOVERY_REQUIRED`에는 100%가 없다.
- 애니메이션 값은 서버/API/DB/log/analytics에 보내지 않고 실제 AI percent로 label하지 않는다.
- 어느 단계에서든 물리·서버 불일치나 최종 반영 실패가 확인되면 `RECOVERY_REQUIRED`로 전환한다.
- AI `DAMAGED | UNCERTAIN | FAILED`는 자동 파손·과금이나 물리 반납 전체 실패가 아니다. Pi가 허용하면 물리 반납을 계속하고 관리자 확인 안내를 표시한다.

**대여 흐름**: 대여 선택 → 얼굴 인증(`KSK-AUTH-001`) → 자격 확인 → 슬롯 안내(Spring 원자 선정, Kiosk가 슬롯 선택 안 함) → 잠금 해제 → 인출 대기 → 재잠금 → 완료. "꺼냈어요" 클릭은 로컬 UI 액션일 뿐 완료 이벤트가 아니다. 완료 조건 = 인출 감지 + 재잠금 확인 + 최종 Result 수신 + Spring/MySQL 저장 성공(v2.0판 원칙 유지, "Spring/MySQL 저장 성공" 확인 경로가 REST 응답에서 WebSocket 최종 Result로 바뀐 점만 다름).

**보류/제외 판단** (v2.0판 유지, 06·09번 v3.0에서 재확인 서술 없음 — 잠정 유지)
| 기능 | 분류 |
|---|---|
| 키오스크 관리자 모드(로컬 진단) | `DEFERRED_NOT_CONTRACTED` 후보, MVP 포함 여부 확인 필요 |
| 슬롯별 개별 잠금 제어 | 일반 사용자 기능 아님 |
| 팬 제어 | `OUT_OF_SCOPE` 확정(잠정 유지) |

### 5.5 담당하지 않는 범위 (변경 없음, 06번 §3.11 표현과 일치)
- 얼굴 검출·임베딩·로컬 사용자 매칭
- 얼굴 템플릿 저장·삭제의 실제 처리
- 대여 자격 최종 판정
- 슬롯 원자적 선정
- GPIO·센서·모터·LED 직접 제어
- 잠금·팬 직접 제어
- 우산 삽입 센서 판정
- 파손 AI 추론과 신뢰도 계산
- 우산·슬롯·대여·반납 상태의 최종 DB 전이
- 결제 승인 결과의 서버 검증
- 관리자 파손 최종 판정
- 지도 재고 집계 로직

---

## 6. 반납 결과 표현 규칙 (변경 없음, 06번 §4.2에서 취지 재확인)

| AI 결과 | 사용자 표시 문구 |
|---|---|
| `NORMAL` | "반납이 완료되었습니다." |
| `DAMAGED` / `UNCERTAIN` / `FAILED` | "반납은 완료되었으며 우산 상태는 관리자가 확인합니다." (확정 파손·비용 문구 금지) |
| 장치·DB 부분 실패 | "반납 상태를 확인 중입니다. 우산을 다시 꺼내지 마세요." |
| 삽입·잠금 실패 | 관리자/운영자 호출 안내 |

> ⚠️ **재확인 필요(v2.0판부터 이어짐)**: `UNLOCK_ERROR`/`LOCK_ERROR` 분리 여부는 06·09번 v3.0에서도 명시적으로 재확인되지 않았다. 17장의 `LOCK_OPERATION_FAILED` 통합 코드 추정은 여전히 미확정.

---

## 7. 상태 코드 규칙 (v3.0)

- `ReturnAttemptStatus` = `PROCESSING` / `PHYSICAL_DONE` / `COMPLETED` / `RECOVERY_REQUIRED` / `FAILED` — 06번 §2.1에서 문자 그대로 재확인.
- `InspectionUiStep`(06번 명칭, v2.0판 `KioskReturnStep`에 대응) 9종: `INSPECTION_GUIDE | INSPECTION_CAPTURE | INSPECTION_PROCESSING | RETURN_SLOT_READY | WAITING_FOR_ITEM_INSERTION | LOCKING_RETURN_SLOT | PHYSICAL_RETURN_DONE | RETURN_COMPLETED | RECOVERY_REQUIRED` — 06번 §2.1에 정식 타입명으로 등장. **타입명이 `KioskReturnStep`에서 `InspectionUiStep`으로 바뀌었을 가능성이 있다** — 로컬 코드(`types/`)의 실제 타입명과 대조 필요. 09번 v3.0은 유사한 값을 화면 Variant 이름(`RETURN_SLOT_GUIDE`, `WAITING_FOR_UMBRELLA_INSERTION` 등)으로 서술하며 06번과 표기가 약간 다르다 — 두 문서 표기 차이 자체를 재확인 필요 항목으로 남긴다.
- `SlotServiceStatus` = `AVAILABLE | ADMIN_REVIEW | OUT_OF_SERVICE` (06번 §2.1, 명시적 3종 확인 — v2.0판엔 이 정확한 3종 열거가 없었음)
- `SlotOccupancyStatus` = `EMPTY | OCCUPIED | UNKNOWN`
- `LockStatus` = `LOCKED | UNLOCKED | UNKNOWN | ERROR`
- `SlotItemCondition` = `EMPTY | NORMAL | DAMAGED | REPAIRABLE | UNKNOWN`
- `InspectionResult` = `NORMAL | DAMAGED | UNCERTAIN | FAILED`
- `RentalStatus` = `REQUESTED | ACTIVE | RETURNING | COMPLETED | LOST | CANCELLED | FAILED` (06번 §2.1 — v2.0판엔 이 전체 열거가 없었음, 신규 확인)
- `UmbrellaStatus`, `umbrellaId`, 이미지 참조/URL 타입은 만들지 않음(변경 없음)
- 알 수 없는 상태 코드 수신 시 임의로 정상 처리하지 않고 `UNKNOWN` 표시 + 로깅(변경 없음)
- `ReturnAttemptStatus`와 `InspectionUiStep`을 합치지 않는다. `processStatus`는 Orin 로컬 추론 메타데이터이며 두 타입을 대신하지 않는다(06번 §2.1).

> ⚠️ **v2.0판 "10번 문서 §6.5의 16종 Kiosk 세션 상태 vs 12번 §3.2의 9종 Inspection UI Step" 이원화 문제**: 06번 v3.0은 9종 쪽(`InspectionUiStep`)만 명시하고 16종 쪽은 언급하지 않는다. 10번 문서가 v3.0에서도 살아있는지, 16종 목록이 갱신됐는지는 이번 확인으로 알 수 없다 — 여전히 재확인 필요.

---

## 8. 진행 원칙 (변경 없음)

1. **화면/코드 설계는 항상 구조·계획·리스크 정리 → 승인 → 진행** 순서. 승인 전 파일 생성/변경 없음.
2. 화면 설계 시 관련 요구사항 ID(FR-xxx), API 연동 지점, Backend 담당자를 함께 표기
3. 확정 안 된 정책(대여 연장, 개별 잠금 등)은 임의 구현하지 않고 **DECISION_REQUIRED**로 별도 관리
4. 문서 근거와 이전 발언이 충돌하면 출처를 밝히며 즉시 플래그
5. 모든 비동기 처리 단계에 로딩/처리 중 표시 필수, 타임아웃 후 임의 완료 처리 금지 → 재조회로 최종 상태 확인
6. 제출 버튼은 클릭 직후 비활성화(중복 요청 방지), `sessionId`/`requestId`/`correlationId` 추적(v3.0에서 WebSocket 상관 키로 `correlationId`가 추가됨, 06번 §2.2.1)

### 와이어프레임 체크리스트 (변경 없음)
1. 사용자 목표·주요 행동 명시
2. 정보 우선순위 (제목→핵심→보조→부가)
3. 화면 간 사용자 흐름 고려
4. Header/Content/BottomNav 등 레이아웃 구조 일관성
5. 시각적 계층 (제목/본문/버튼 구분)
6. 구체적 CTA 문구 (예: '예약하기'; 모호한 '확인/다음' 지양)
7. 8/16/24/32px 정렬된 여백
8. 요소 정렬 (기준선 통일)
9. 버튼·카드 스타일 일관성
10. 실제 콘텐츠 길이(긴 텍스트) 고려
11. 예외 상태(빈 결과/오류/로딩/권한없음/데이터없음) 포함
12. 접근성 (큰 버튼·충분한 글자크기·아이콘+텍스트 병행)
13. 구현 가능성 (개발 난이도·데이터 구조 부합) 검토

---

## 9. DECISION_REQUIRED (v3.0 기준 재정리)

### 9.1 공통 결정 대장 — RESOLVED/SUPERSEDED

| ID | 내용 | 상태 |
|---|---|---|
| `DEC-021` | AI 실행 노드는 Jetson Orin 통합 | RESOLVED (v2.0부터 유지) |
| `DEC-022`/`DEC-023` | 얼굴 등록 이미지 흐름 | RESOLVED (v2.0판 근거, 06번 v3.0엔 상세 재서술 없음 — 3.2절 참고) |
| `DEC-024` | Kiosk FE → Spring Kiosk API만 사용 | **SUPERSEDED** — 06번 v3.0의 "Kiosk Frontend WebSocket Client 전환"으로 폐기됨. Kiosk FE는 다시 Pi WebSocket을 직접 사용한다. |
| `DEC-025` | 정산 정책(24h/1,000원/7일/7,000원) | RESOLVED (변경 근거 없음) |
| `DEC-026` | `ReturnAttemptStatus` 5종 | RESOLVED, 06번 §2.1에서 재확인 |
| `DEC-030` | 실제 대학 SSO·IdP·학생 DB 연동 | `DEFERRED_POST_P0` (변경 없음) |
| `DEC-031` | P0 로컬 시연 계정(`login_id`+`password_hash`) | RESOLVED, 06번 §2.2·§3.1에서 재확인 |
| `DEC-032` | Kiosk FE → Raspberry Pi 직접 WebSocket 연결(얼굴 인증 트리거 한정으로 국지 서술) | **재평가 필요** — 06번 v3.0에 따르면 이 결정의 방향(Pi WebSocket) 자체는 맞았으나, 적용 범위가 "얼굴 인증 트리거 1곳"이 아니라 **Kiosk WebSocket Operation 7개 전체**로 넓어졌다. `DEC-032`를 "1곳 한정 예외"로 계속 서술하면 실제 구현 범위와 어긋난다. 팀 재확인 후 이 항목의 서술 자체를 갱신할 것. |
| `DEC-046` | (v2.0판엔 없던 07-28자 결정, 06번 변경이력에서 발견) Kiosk→Pi Local REST 7 + Kiosk→Orin Preview Stream 최종 통신 기준 동기화 | **`SUPERSEDED_BY_이후_07-29_WebSocket_전환`** — 06번 자체 변경이력에 "당시 기준, 현재는 07-29 WebSocket 전환이 활성" 취지로 남아있음 |
| `DEC-049` | 통신 경계 권위(Kiosk↔Pi WebSocket 등 전체 재정의) | 06번 §8·08번 §1.0에서 "기준 결정: `DEC-049`"로 명시. **v3.0 아키텍처 변경의 공식 근거.** 08번은 이 결정이 통신 아키텍처·Kiosk 7개 계약 권위 이전만 다루고 스키마 변경 근거는 아니라고 명시적으로 범위를 한정한다. |
| `DEC-047` | P1-8 새 ERD 권위선(MySQL 11개 확정, `ADMIN_ACCOUNT` 분리, `USER_ACCOUNT.role` 제거, `DEVICE_OPERATION` Option A) | **RESOLVED — 08번 원문에서 확인됨**(§변경이력 2026-07-29). |
| `DEC-048` | 신원 모델(9자리 학번 `user_id` PK + `user_ref` UUID UNIQUE 가명키) | **RESOLVED — 08번 원문에서 확인됨**(§변경이력 "V13 학번 PK·가명 user_ref 기준선", §2.3·§7.1). 상세는 3.3절 참고. |
| `DEC-050` | 데이터·무결성 권위(V18 스키마 기준선: MySQL 업무 11+내부 Guard 1=물리 12, Trigger 9, Orin 1, Pi 0) | **RESOLVED — 08번 원문에서 확인됨**(§1.0·§11.0·§19.5, `DEC-050 V18_SCHEMA_INTEGRITY_BASELINE`). 상세는 12.1절 참고. |

> ⚠️ **`DEC-032`의 위상 변화**: v2.0판 지침서는 `DEC-032`를 "00번 공식 변경이력에 없는, 팀이 이 브랜치에서 직접 내린 국지적 예외"로 취급했다. 이번 06번 원문 확인 결과, 방향 자체는 이후 공식 문서(`DEC-049`, 2026-07-29)와 일치하지만 **적용 범위 서술은 여전히 06번보다 좁다.** `DEC-032`를 폐기하지 말고, "얼굴 인증 트리거 1곳"이라는 한정 문구를 "Kiosk WebSocket Operation 7개 전체"로 넓히는 재작성이 필요하다 — 이는 코드 구현 범위에도 영향을 준다(현재 코드가 얼굴 인증에서만 WebSocket을 쓰고 나머지 6개 오퍼레이션에서 REST를 쓰고 있다면 수정 필요).

### 9.2 아직 열려 있는 항목 (Hyejin 관련, 06·09·08번 및 12-I 확인 기준 재정리)

| 항목 | 현재 해석 | 확정 시점 |
|---|---|---|
| 모바일/키오스크 저장소 구조 | monorepo 권장 vs 분리 (06번 ARCHIVED §9에서도 미확정으로 남음) | 1일차 (미확정) |
| 키오스크 UI 런타임 | React Web/PWA — 06번 §0.0 기술스택 표에 "React Native, React, TypeScript"로만 명시, PWA 확정 여부는 명시적 재확인 없음 | 미확정 |
| 얼굴 등록 Kiosk Operation 공백 | `KSK-AUTH-001` 재사용 여부 불명(06·09·12-I에서도 미해소) | 착수 전 필수 확인 |
| Kiosk WebSocket Heartbeat 수치 | **해소됨(2026-07-30)** — 12-I 확인 결과 Ping/Pong 10초 간격, 30초 무응답 시 `KIOSK_WEBSOCKET_UNAVAILABLE` | 해소 |
| Kiosk WebSocket 재연결 최대 횟수·지수 Backoff 계수·Jitter 범위 | 12-I 본문 전량 확인(2026-07-30)에도 수치 없음 | 12-I 담당자 재질의 필요 |
| 자동 모달 닫기 시간 확정 여부 | 06·09번에서 재확인 없음(09번은 `PROVISIONAL_UI_VALUE`로만 표시) | 미정 |
| 얼굴 정렬 안내 코드 제공 방식 | `KSK-AUTH-001`이 `FaceGuidance`를 포함한다고 서술(06번 §4.1) — 필드명은 `faceGuidance`로 추정되나 명시적 재확인 없음 | 미정 |
| 실제 AI 진행률 제공 가능 여부 | 06·09번은 `progressMode=INDETERMINATE`와 0~90% capped animation을 **활성 규칙으로 명시** — v2.0판의 "불가하면 INDETERMINATE만 사용"이라는 조건부 서술보다 한 단계 더 확정된 것으로 보임 | 사실상 확정(§5.4 참고), 팀 재확인 권장 |
| 로컬 슬롯 진단 MVP 포함 여부 | `KSK-DIAG-001` 미확정 (06번 v3.0 활성 본문에 재서술 없음, 12-I 개별 페이지도 없음) | 개발 시작 전 |
| 팬 제어 제외 유지 여부 | `KSK-FAN-001 OUT_OF_SCOPE` 유지 전망(06번 v3.0 활성 본문에 재서술 없음, 12-I 개별 페이지도 없음) | 확정적 아님 |
| 키오스크 폼팩터 | **해소됨** — "터치 입력 지원 노트북 화면"으로 확정(06번 §2.6, 변경이력 2026-07-28) | 해소 |
| 지도 SDK | 미정 | 미정 |
| 관리자 실시간 갱신 방식 | Polling 우선 제안(06번 ARCHIVED에서도 동일) | 3일차 |
| `DEC-048` 신원 모델(9자리 학번 PK + 내부 가명 UUID) | **해소됨** — 08번 원문 확인 완료(2026-07-30). `user_id CHAR(9)` PK, `user_ref CHAR(36)` UUID UNIQUE(PK 아님), Orin `FACE_PROFILE.user_ref UUID`가 PK. 상세 3.3절 | 해소 |
| `DEC-050` 데이터·무결성 권위(V18 스키마 기준선) | **해소됨** — 08번 원문 확인 완료(2026-07-30). MySQL 업무 11+내부 Guard 1=물리 12, Trigger 9개(명칭 확인), Orin 1, Pi 0. 단 "V1~V18" 개별 마이그레이션 내역은 문서에 없고 첨부 SQL 파일이 권위. 상세 12.1·13절 | 해소(세부 마이그레이션 목록은 여전히 미확인) |
| 12-R 본문 상세(App/Admin REST 23개 세부 스키마) | 08번·12-I 확인으로도 해소되지 않음 — 12-R은 이번 작업 범위 밖 | 미확인, 12-R 원문 확인 필요 |
| v1.0 반납 Variant(`UMBRELLA_NOT_DETECTED`/`INSERTION_INCOMPLETE`/`LOCK_FAILED`)의 v3.0 존속 여부 | 09번 v3.0 화면 카탈로그에 독립 항목으로 없음, 동작 원칙은 유지된다고만 서술 — 흡수/삭제 여부 불명 | 09번 원문 추가 확인 필요, 임의로 흡수·삭제 단정 금지 |

---

## 10. 개발 일정 (변경 없음)

> ⚠️ v1.0판의 상세 일별 일정표는 여전히 근거 문서가 없다. 06번 v3.0에도 날짜별 일정표 대신 §7 "통합 테스트·완료 정의" 체크리스트만 있으며, 이 체크리스트는 v3.0 WebSocket 전환에 맞춰 갱신됐다(아래 신규 항목 반영):
> - App REST 13개와 Admin REST 10개 호출 경계가 변경되지 않았다
> - **Kiosk 코드에 Pi Local REST Client·Method·Path 호출이 없고 7개 `KSK-*` ID가 WebSocket Operation Handler로 존재한다**
> - **Kiosk WebSocket 연결 생성·인증·Heartbeat·Timeout·지수 Backoff/Jitter 재연결을 시험한다**
> - **ACK와 최종 Result/Terminal을 구분하며, 최종 성공 메시지 전에는 100%나 완료를 표시하지 않는다**
> - Route 재진입·React Strict Mode·재렌더링 후에도 Listener가 중복 등록되지 않는다
> - 화면 이동·Unmount·세션 완료·취소에서 Subscription·Handler가 정리된다
> - 연결 단절 UX가 입력을 잠그고 성공을 추정하지 않으며 `KIOSK_WEBSOCKET_UNAVAILABLE`을 표시한다
> - 재연결 후 기존 `sessionId`로 `KSK-SESSION-002` Snapshot을 재조회하고 화면 상태를 조정한다
> - **Kiosk가 Spring 또는 Orin Endpoint를 직접 호출하지 않는다**
> - Frontend bundle·환경 변수·로그에 MQTT Topic/Client 인증서·장치 WebSocket 자격 증명이 없다
> - `MQTT_BROKER_UNAVAILABLE`, `KIOSK_WEBSOCKET_UNAVAILABLE`, `ORIN_WEBSOCKET_UNAVAILABLE`을 서로 다른 사용자 상태·운영 로그로 표현한다
> - 챗봇·결제·비밀번호 관련 항목은 v2.0판과 동일
>
> **DECISION_REQUIRED로 등록**: 날짜별 실행 일정은 팀이 새로 짜야 한다. 개발 기간(2026-07-24~08-07, 11 Working Day) 자체는 변경 없음.

---

## 11. 미확인 영역 (다음 확인 필요, v3.0 기준 갱신)

- 00번 통합 변경 이력 v3.0 — 미확인 (v2.0판만 확인함, `DEC-046`~`DEC-050` 전체 이력을 원문으로 볼 필요 있음)
- 01/02/03번(PRD·요구사항·유저스토리) v3.0 — 미확인
- 04번 정책 명세서 v3.0 — 미확인 (정산 값 자체는 06번에서도 변경 근거 없음)
- 05/07번 Backend·임베디드 역할 명세서 — 미확인
- 08번 ERD v3.0 — **원문 확인 완료**(138,145자, 2026-07-30). `DEC-048`(신원 모델)·`DEC-050`(V18 스키마 기준선, 테이블·Trigger 구성)의 근거 문서임을 확인, 3.2·3.3·12.1·13절에 반영. 단 08번은 스키마·데이터 문서이므로 12-I의 WebSocket Heartbeat/Timeout 수치 등은 다루지 않는다 — 그 부분은 12-I로 별도 확인(아래 참고).
- 09번 화면 흐름 명세 — **v3.0 원문 확인 완료**(`docs/09-screen-flow.md`에 Kiosk 부분 반영, 2026-07-30). 단 `UMBRELLA_NOT_DETECTED`/`INSERTION_INCOMPLETE`/`LOCK_FAILED` 3개 Variant의 v3.0 존속 여부 등 일부 세부는 09번 자체 문서에도 "원문 확인 필요"로 남아있음.
- 10번 — "비권위·화면추적용"으로 격하됐다는 것이 사용자 배경 정보이지 06·09번 원문 확인 사항이 아님 — 재확인 필요
- 11번 관리자 웹 화면흐름 — 미확인 (Hyejin 담당 아님)
- 12번, 12-R — 06·09번이 인용하는 수준(계약 수량·mention-page)만 확인, 본문 미확인.
- **12-I(장치·내부 인터페이스 계약 DB) — 2026-07-30 Kiosk↔Pi WebSocket 7개 Operation 개별 페이지 전체 + 상위 공통 문서("P0 구현 기준") 전량 확인 완료.** 목록·엔드포인트뿐 아니라 messageType(Request/ACK/Event/Result/Error)·핵심 필드·오류 코드·Timeout·멱등성 규칙, 공통 Envelope(`schemaVersion,messageType,messageId,operationId,sessionId,requestId,correlationId,sentAt,expiresAt,sequence,payload`), Heartbeat(Ping/Pong 10초, 30초 무응답 시 `KIOSK_WEBSOCKET_UNAVAILABLE`)까지 확인해 `docs/09-screen-flow.md`(3.1~3.6절)·본 문서(3.1·5.4.1절)에 반영했다. **재연결 최대 횟수·지수 Backoff 계수·Jitter 범위는 12-I 본문 전체를 검색해도 없어 여전히 미확인**(추측 금지, 임의 수치 하드코딩 금지). Spring↔Pi MQTT 10개(`EDGE-*`)·Pi↔Orin WebSocket 부분은 이번 확인 중 참고 수준으로만 스쳤고 본격 확인 대상은 아니었다(Frontend 1 직접 호출 범위 밖이므로 우선순위 낮음).

---

## 12. ERD·API 명세 핵심 요약 (v3.0, 06·09·08번 문서 및 12-I 확인 기준으로 갱신)

### 12.1 데이터 저장소 구조 (**08번 ERD v3.0 원문 전문 확인 완료**, 2026-07-30, 138,145자)

**MySQL 업무 SSOT 테이블 11개**(08번 §1.0·§7.1·§11.0·§19.5 확인) — 로컬 지침서 v2.0판 서술이었던 9개 목록보다 **3개 많다**:
`ADMIN_ACCOUNT`, `USER_ACCOUNT`, `FACE_PROFILE_SYNC_OPERATION`, `STATION`, `SLOT`, `RENTAL`, `RETURN_ATTEMPT`, `DAMAGE_INSPECTION`, `SETTLEMENT`, `PAYMENT_ATTEMPT`, `DEVICE_OPERATION`

**MySQL 내부 물리 테이블(업무 SSOT 집계 밖) 1개**: `SETTLEMENT_PAYMENT_MUTATION_GUARD` — `PAYMENT_ATTEMPT` Trigger가 `SETTLEMENT.paid_amount/status/paid_at` 갱신 시에만 쓰는 1회성 handshake 테이블. 애플리케이션 DML 대상이 아니다.

**MySQL 물리 테이블 합계 = 11 + 1 = 12개.** Orin PostgreSQL + pgvector 업무 SSOT는 `FACE_PROFILE` 1개(PK `user_ref UUID`). Raspberry Pi는 **관계형 DB·SQLite·영속 Command/Event Journal을 0개** 갖는다(설정 파일·프로세스 메모리·순환 로그만 사용, 08번 §2.2·§7.2·§19.5).

**Trigger 9개**(08번 §11.0에서 명칭까지 확인):
`TRG_SETTLEMENT_BEFORE_INSERT`, `TRG_SETTLEMENT_PAYMENT_BEFORE_UPDATE`, `TRG_SETTLEMENT_DAMAGE_BASIS_BEFORE_DELETE`, `TRG_DAMAGE_INSPECTION_REVIEW_AUDIT_BEFORE_UPDATE`, `TRG_RETURN_ATTEMPT_BEFORE_INSERT`, `TRG_RETURN_ATTEMPT_BEFORE_UPDATE`, `TRG_PAYMENT_ATTEMPT_CREDIT_INSERT`, `TRG_PAYMENT_ATTEMPT_CREDIT_UPDATE`, `TRG_PAYMENT_ATTEMPT_BEFORE_DELETE`

**Flyway V1~V18 관련 주의**: 08번은 물리 스키마 권위를 "첨부 V1~V18 clean-install SQL"(`billisan_backend3_p0_p1_final_realfinal_schema.sql`)이라고 명시하지만, **버전 번호별(V1, V2, ... V18) 개별 변경 내역을 한 줄씩 나열한 목록은 본문에 없다.** 본문에서 실제로 버전 번호가 명시된 지점은 `DEC-048 = V13`(학번 PK·가명 user_ref 기준선, 2026-07-29)과 `DEC-050 = V18`(`V18_SCHEMA_INTEGRITY_BASELINE`, 최종 기준선)뿐이다. V1~V12, V14~V17이 각각 무엇을 변경했는지는 첨부 SQL 파일 자체를 봐야 하며 08번 텍스트만으로는 확인 불가 — **여전히 미확인**.

**주요 테이블 구조 확인 사항(08번 §7.1·§7.3)**:
- `USER_ACCOUNT`: `user_id CHAR(9)`가 PK(9자리 숫자 학번), `user_ref CHAR(36)`는 UNIQUE 가명 UUID(PK 아님), `login_id VARCHAR(254)`(대학 이메일, UNIQUE), `password_hash VARCHAR(255)` 존재. `student_number`, `role`, `account_status` 컬럼은 없음.
- `FACE_PROFILE`(Orin PostgreSQL): PK가 `user_ref UUID`. `face_profile_id`·`registration_status` 컬럼 없음, 행 존재 여부가 등록 여부의 최종 권위.
- `DEVICE_OPERATION`: `operation_id` PK, `command_id`/`event_id`만 UNIQUE로 저장하고 **`request_id`·`rental_id`·`return_attempt_id`·`business_id`·`correlation_id` 컬럼이 없다** — 독립 물리 장치 Command/Terminal 원장이며 업무 관계를 중복 저장하지 않는다. 업무 `request_id`와의 상관은 Spring 트랜잭션·로그 컨텍스트에서만 이뤄진다.
- `STATION`: `station_id CHAR(36)`가 PK이자 "MQTT Topic·장치 설정·인터페이스 상관의 권위 식별자"(08번 §7.1). `station_code`·`location_text`는 **P0 필수 컬럼이 아니다**(`name`만 표시용으로 존재).
- `DAMAGE_INSPECTION.reviewed_by CHAR(36)`는 `ADMIN_ACCOUNT.admin_id`를 `ON UPDATE RESTRICT · ON DELETE RESTRICT`로 참조 — 로컬 문서 v2.0판 서술과 일치, 08번으로 재확인.

**로컬 지침서 §12.1(구) 대비 테이블 목록 대조 결과**: 기존 9개 목록(`USER_ACCOUNT`, `STATION`, `SLOT`, `RENTAL`, `RETURN_ATTEMPT`, `DAMAGE_INSPECTION`, `SETTLEMENT`, `PAYMENT_ATTEMPT`, `DEVICE_OPERATION`) 자체는 오류 없이 전부 08번에도 존재하지만, **`ADMIN_ACCOUNT`, `FACE_PROFILE_SYNC_OPERATION`, `SETTLEMENT_PAYMENT_MUTATION_GUARD` 3개가 누락**되어 있었다. 이 갱신으로 목록을 11(업무)+1(내부 Guard)=12로 정정한다.

- `UMBRELLA`/`umbrellaId` 엔터티 없음 — `SLOT` 단위로만 재고 관리(06·09·08번 모두 확인, 08번 §6.2 "개별 우산 엔터티 제거 결정")
- Raspberry Pi: 관계형 DB 없음(06번 §1.1 간접 확인 + **08번 §2.2·§7.2·§19.5에서 명시적으로 재확인**: "Pi는 Table·SQLite·영속 Command/Event Journal을 갖지 않는다")

### 12.2 통신 경계 구조 (v3.0, 06번 §1·§1.1·§6, 09번 §2.5·§3 기준 — **Base 체계 전면 변경**)

| 경계 | 방식 | 상태 |
|---|---|---|
| App → Spring | HTTP REST `/api/v1` 계열, 13개 | `CONFIRMED`, 12-R 권위 |
| 관리자 Web → Spring | HTTP REST, 10개 | `CONFIRMED`, 12-R 권위 |
| **Kiosk FE → Raspberry Pi** | **WebSocket**, Operation 7개 (HTTP 아님) | `CONFIRMED`, 12-I 권위(스키마 상세 확인 완료) |
| Spring ↔ Pi | MQTT(EC2 Mosquitto), Operation 10개 | `CONFIRMED`, 12-I 권위, Frontend 직접 호출 안 함(이번 확인 범위 밖) |
| Pi ↔ Orin | 로컬 WebSocket | Frontend 직접 연결 금지, 40개 집계 밖(이번 확인 범위 밖) |

**Frontend 1이 직접 다루는 계약(v3.0)**
- App 13개(HTTP REST, Spring 직접 호출): `AUTH-001/002`, `USER-001`, `HOME-001`, `RENTAL-001/002`, `SETTLEMENT-001`, `PAYMENT-001/002/003`, `ALERT-001`, `WEATHER-001`, `CHATBOT-001`
- Kiosk 7개(**WebSocket Operation, Pi 직접 연결**): `KSK-STATION-001`, `KSK-SESSION-001/002`, `KSK-AUTH-001`, `KSK-RENT-001`, `KSK-RETURN-001`, `KSK-CANCEL-001`

### 12.3 OPEN DECISION (06번 §9 "확정 필요 사항" 기준 발췌 — ARCHIVED 절 내용이라 v3.0 활성 여부 불확실, 참고용)

| 항목 | 비고 |
|---|---|
| 키오스크 UI 런타임(React Web/PWA vs RN) | 06번 ARCHIVED §9에 여전히 미확정으로 등장 |
| 키오스크 세션 상태 전달 방식(Polling/SSE/WebSocket) | ARCHIVED §9에 "미정"으로 등장 — 단, 활성 §1·§4.3 본문은 이미 WebSocket을 전제로 서술하고 있어 사실상 WebSocket으로 굳어진 것으로 보임. 09번 v3.0에도 동일한 내부 모순(`DEC-SF-015`)이 있음 — ARCHIVED와 활성 본문 간 불일치이므로 팀 재확인 필요 |
| 팬 제어 | `FR-DRY-001` Could, MVP 포함 여부 미정 |
| 관리자 실시간 갱신 | Polling 우선 제안 |

### 12.4 API 명세에서 확정 제외(`OUT_OF_SCOPE`/`DEFERRED_NOT_CONTRACTED`, 06번 §3.3·§5.3 기준, 변경 없음)
- 대여 연장, 팬 제어(`KSK-FAN-001`), 관리자 웹 대시보드 확장 기능 일부, CSV/XLSX 등
- App 지도(`STATION-001/002`)는 P1 후보로 유지

### 12.5 정산 API 규칙 (변경 없음)
- 대여 완료 후 24시간 무료 → 이후 1일 1,000원 → 연체 7일 시 `LOSS` 7,000원 → 관리자 파손 확정 시 `DAMAGE` 7,000원 → 파손·연체 비합산, 대여 1건 총액 최대 7,000원

---

## 13. 리스크·정보 공백 종합 (v3.0 갱신)

### 13.1 문서 간 모순 (재확인 필요)

| # | 내용 | 근거 | 조치 |
|---|---|---|---|
| 1 | **키오스크 얼굴 등록 Operation 누락** | v1.0부터 이어진 문제. 06·09번 v3.0의 Kiosk WebSocket Operation 7개, 12-I 개별 페이지 어디에도 등록 전용 Operation이 없음 | 5.4·5.4.1절에 반영, Backend/Embedded 확인 필요 — 가장 오래 방치된 공백 |
| 2 | **Kiosk Enum 이원화(10번 16종 vs 12번/06번 9종)** | 06번은 9종(`InspectionUiStep`)만 서술, 10번의 16종 재확인 여부 불명 | 7장에 반영, 9종 기준으로 구현하되 10번 v3.0 재확인 필요 |
| 3 | **잠금 해제/잠금 실패 코드 분리 여부** | 06·09번 v3.0에서도 명시적 재확인 없음 | 6장·17장에 반영, Backend 확인 필요 |
| 4 | **`DEC-032`의 적용 범위 서술 불일치** | v2.0판은 "얼굴 인증 트리거 1곳"으로 좁게 서술, 06번 v3.0은 Kiosk WebSocket Operation 7개 전체를 전제 | 3장·9.1절에 반영, 팀 재확인 후 `DEC-032` 서술 자체를 갱신할 것 |
| 5 | **ARCHIVED 절(구 v2.1 내용) vs 활성 절(v3.0) 간 불일치** | 06번 문서 내부에 "세션 상태 전달 Polling/SSE/WebSocket 미정"(ARCHIVED §9)이라는 서술과 "WebSocket 전제"(활성 §1·§4.3)인 서술이 공존. 09번의 `DEC-SF-015`도 동일한 성격의 모순 | 문서 자체의 내부 정합성 문제, 06·09번 문서 관리자에게 확인 필요 |
| 6 | **키오스크 폼팩터·저장소 구조·UI 런타임** | 폼팩터는 이번에 해소(노트북 화면 확정), 저장소 구조·UI 런타임은 여전히 미확정 | 9.2절 참고, 팀 재확인 필요 |
| 7 | **v1.0 반납 Variant 3종의 v3.0 존속 여부** | `UMBRELLA_NOT_DETECTED`/`INSERTION_INCOMPLETE`/`LOCK_FAILED`가 09번 v3.0 화면 카탈로그에 독립 항목으로 없음 | 09-screen-flow.md 6절 참고, 원문 추가 확인 전 임의 흡수·삭제 금지 |
| 8 | **로컬 지침서 v2.0판 MySQL 테이블 목록 누락 (해소됨)** | 08번 원문 확인 결과, 기존 §12.1의 9개 목록에서 `ADMIN_ACCOUNT`·`FACE_PROFILE_SYNC_OPERATION`·`SETTLEMENT_PAYMENT_MUTATION_GUARD` 3개가 빠져 있었다(실제 11업무+1내부=12개) | 12.1절에 정정 반영 완료. FE 관점에서는 관리자 인증 분리(`ADMIN_ACCOUNT`)와 얼굴 동기화 상태 테이블(`FACE_PROFILE_SYNC_OPERATION`) 존재를 인지할 필요 |

### 13.2 v3.0 문서 자체가 아직 검토 미승인 상태라는 리스크

- 06·09번을 포함한 v3.0 문서군 전체가 `REVIEW_REQUIRED · NOT_RELEASED`다. v2.0 때처럼 "팀이 확인 후 로컬 기준선으로 채택"하는 절차가 아직 이 프로젝트에서 이루어지지 않았다 — 이 지침서 갱신은 그 채택을 대신하지 않는다.
- 06번 문서 자체 헤더는 `CORRECTION_STATUS=APPLIED`인데 문서군 전체 플래그(사용자 제공 배경 정보 기준)는 `IN_PROGRESS`다 — 이 불일치의 의미(06번만 먼저 교정 완료된 것인지, 표기 오류인지)는 확인되지 않았다.
- 06번 §0.0의 `PREVIOUS_BASELINE=v3.0` 표기는 자기 자신을 가리키는 것처럼 보여 오기(誤記) 가능성이 있다 — 원문 그대로 인용하되 의미는 확정하지 않는다.

### 13.3 아직 안 읽은 문서로 인한 공백

- **08번 ERD v3.0**: **원문 확인 완료**(2026-07-30). `DEC-048`(신원 모델)·`DEC-050`(데이터·무결성, V18 스키마 기준선)의 근거 문서임이 확인됐다. 남은 공백: (1) V1~V18 개별 마이그레이션 파일의 버전별 변경 내역은 첨부 SQL 파일에만 있고 08번 본문 텍스트에는 없음, (2) 08번은 스키마·데이터 문서이므로 12-I의 WebSocket 수명주기 수치(Heartbeat·Timeout 등)는 08번 범위 밖이었다 — 이 부분은 아래 12-I 항목에서 별도로 확인됨.
- **12-I(장치·내부 인터페이스 계약 DB)**: **2026-07-30 확인 완료.** Kiosk WebSocket 7개의 목록·엔드포인트뿐 아니라 messageType, 요청/ACK/Event/Result 필드, 오류 코드, Timeout, 공통 Envelope, Heartbeat(10초/30초) 수치까지 12-I 개별 Operation 페이지 7개와 상위 공통 문서("P0 구현 기준")를 전량 읽어 확인했다(3.1·5.4.1절, `docs/09-screen-flow.md` 3.1~3.6절 반영). **재연결 최대 횟수·지수 Backoff 계수·Jitter 범위만은 12-I 문서 전체에 없어 여전히 미확인** — 이 세 수치는 다음 확인 대상으로 유지(12-I 담당자에게 직접 질의 필요, 다른 문서에 있을 가능성은 낮아 보이나 단정하지 않음).
- **12-R(REST API 계약 DB)**: App/Admin 23개 상세, Frontend 1의 App 담당 부분에 필요하나 우선순위는 낮음(App은 이번 변경의 핵심이 아님). 이번 08번·12-I 확인 작업 범위 밖.
- **05/07번 Backend·임베디드 역할 명세서**: MQTT Operation 10개·Pi↔Orin WebSocket 계약의 세부 구현 배정 미확인. (12-I 공통 문서에 Spring↔Pi MQTT·Pi↔Orin WebSocket 관련 절도 있으나 이번 작업은 Kiosk WebSocket 7개로 범위를 한정했다 — 05/07번 자체를 읽은 것은 아니다.)

### 13.4 구현 착수 전 명확히 해둘 세부사항

- **Kiosk WebSocket Heartbeat**: 12-I 확인 완료 — Ping/Pong 10초 간격, 30초 무응답 시 `KIOSK_WEBSOCKET_UNAVAILABLE`(2026-07-30, 3.1절 반영). **재연결 최대 횟수·지수 Backoff 계수·Jitter 범위는 12-I 전체를 확인해도 없어 여전히 미확인** — 값을 하드코딩하지 말고 팀 확인 후 별도 결정 필요.
- **`clientSessionKey`**: Web Crypto로 생성, sessionStorage에만 저장, 사용자 정보 미포함(06번 §4.3) — 로컬 코드가 이 규칙을 따르는지 점검 필요.
- **ACK/최종 Result 구분**: 로컬 코드가 현재 WebSocket 메시지 처리에서 ACK와 최종 Result를 구분하고 있는지, `progressMode=INDETERMINATE` + 0~90% capped animation 규칙을 따르는지 점검 필요(현재 `PHASE.md`상 Phase 3 반납 진행 중이므로 이 규칙이 직접 적용되는 시점).
- **Toss 딥링크(`billisan://payments/success`)**: RN `Linking` 설정 필요(변경 없음).

---

## 14. 디자인 시스템 (아키텍처 변경과 무관, v1.0판 유지 — 이번 06·09·08번 확인으로 갱신 없음)

### 14.1 컬러 팔레트

**핵심 Primary: `#FFD700`(Gold)** — 키오스크·앱 공통 브랜드 컬러(v2.0판 실측 유지, 06·09·08번은 디자인 토큰 상세를 다루지 않음).

| 구분 | 색상 | 근거 |
|---|---|---|
| **Primary (확정)** | `#FFD700` | 버튼·로고·카운트다운 배지 픽셀 샘플링 일치(2026-07-27 실측) |
| 키오스크 배경(크림톤) | `#FDFAE9` | 실측 |
| 다크 네이비(로고·헤딩 텍스트) | `#11182A` | 실측값 |
| 비활성(disabled) 버튼 | `#D9D9D9` | 실측 |
| 에러 배지 배경(연핑크) | `#FFD8D3` | 실측 |
| 에러 배지 텍스트 | `~#C34E4B` | 근사치 |
| 모바일 배경/본문/보조/CTA | `#F5F7FA`/`#1E2A38`/`#5C6B7A`/`#FFEA63` | 기존 저장값(미실측) |

### 14.2 Figma
- 파일 키: `N502YHzrglrnW1TdEOlgEk`, 페이지 구성: "App", "Kiosk"

### 14.3 와이어프레임 도구
- Pencil CLI(`@pencil.dev/cli`): `pencil --out file.pen --prompt "..."` 형식

---

## 15. 개발 환경·도구 (아키텍처 변경과 무관, 일부 재확인 표시 갱신)

### 15.1 모바일 (React Native + Expo)
- Expo Go(App Store 버전)는 SDK 54와만 호환
- `localhost`는 실기기 기준 실기기 자신을 가리킴 — LAN IP 또는 `--tunnel` 필요

### 15.2 얼굴 인식 라이브러리

> ⚠️ **v3.0에서도 재확인 필요 상태 유지**: Kiosk FE는 Orin에 직접 연결하지 않고(§3, §2.3) Pi WebSocket이 제공하는 결과만 표시한다. `face-api.js`/MediaPipe 등은 여전히 단순 안내용 오버레이(가이드 원 표시) 용도로만 유효하며, 실제 인증 로직으로 쓰면 3장 책임 경계 위반이다.

### 15.3 이미지 처리
- 배경 제거: `rembg`(`u2net` 모델)

### 15.4 Notion 작업 팁
- 이 워크스페이스에서는 한국어 검색어가 결과가 더 잘 나옴
- 대용량 Notion 페이지(06번 95,734자, 09번 130,123자, 08번 138,145자, 12-I 공통 문서 75,727자)는 `notion-fetch` 결과가 파일로 저장되며, python으로 문자 단위 슬라이싱해 전량 읽는 방식이 유효했다.

### 15.5 날씨 API 후보 (미확정)
- Open-Meteo, OpenWeatherMap One Call 3.0/4.0

---

## 16. AI·개인정보 판단 근거 (변경 없음)

- 얼굴 데이터는 원본이든 임베딩이든 한국법상 항상 민감정보로 취급된다. 온디바이스(Edge-only) 처리는 리스크를 줄이고 셀링포인트로 쓸 수 있다.
- 파손 분류 CNN(YOLOv8n + PatchCore, Jetson에서 수행)을 핵심 AI 기여로 포지셔닝할 것.
- `login_id`·평문 비밀번호·`password_hash`도 민감 취급 대상(06번 §2.2·§2.3에서도 재확인).
- v3.0에서 추가로: Kiosk WebSocket `clientSessionKey`, `sessionId`, `requestId`, `correlationId`도 사용자 식별에 사용되지 않는 무작위 값이어야 한다는 원칙이 명시됨(06번 §2.3·§4.3).

---

## 17. 핵심 오류 코드 (v3.0 — WebSocket 관련 코드 추가)

| 코드 | HTTP/전달 방식 | retryable | 의미·화면 행동 |
|---|---|---|---|
| `INVALID_CREDENTIALS` | 401 | — | 로그인 정보 오류 |
| `ACCOUNT_UNAVAILABLE` | 403 | — | 정지·탈퇴 등 이용 불가 |
| `FACE_NOT_DETECTED` | Kiosk WS 메시지 | true | 위치 안내 후 새 인증 요청 |
| `FACE_NOT_MATCHED` | Kiosk WS 메시지 | true | 등록 얼굴 불일치, 명시적 재시도 |
| `ORIN_UNAVAILABLE`(구 `FACE_AUTH_UNAVAILABLE`) | Kiosk WS 메시지 | true | Orin·카메라·추론 사용 불가 |
| `AUTH_TIMEOUT` | Kiosk WS 메시지 | true | 인증 세션 시간 초과 |
| **`KIOSK_WEBSOCKET_UNAVAILABLE`** | WebSocket 연결 계층 | true(재연결) | **신규(06번 §3·§4.3)**. Kiosk↔Pi 연결 단절. 조작 잠금 + "연결이 끊어졌습니다" 안내, 재연결 후 Snapshot 재조회 |
| **`MQTT_BROKER_UNAVAILABLE`** | Spring↔Pi 내부(FE는 간접 인지) | — | **신규(06번 §3·§1.1)**. Spring↔Pi MQTT 단절. FE가 직접 받는 오류는 아니지만 원인 구분용으로 인지 필요 |
| **`ORIN_WEBSOCKET_UNAVAILABLE`** | Pi↔Orin 내부(FE는 간접 인지) | — | **신규(06번 §3·§1.1)**. Pi↔Orin 연결 단절. `ORIN_UNAVAILABLE`과 구분되는 내부 원인 코드로 추정 |
| `ACTIVE_RENTAL_EXISTS` | Kiosk WS 메시지 | false | 신규 대여 차단 |
| `ACTIVE_RENTAL_NOT_FOUND` | Kiosk WS 메시지 | false | 반납 대상 없음 안내 |
| `UNSETTLED_EXISTS` | Kiosk WS 메시지 | false | App 결제 경로 안내 |
| `NO_RENTAL_SLOT` / `NO_RETURN_SLOT` | Kiosk WS 메시지 | true | 대여/반납 불가, Station Summary 재조회 |
| `STATION_UNAVAILABLE` / `DEVICE_OFFLINE` | Kiosk WS 메시지 | true | 점검·오프라인 안내, 물리 동작 금지 |
| `LOCK_OPERATION_FAILED` | Kiosk WS 메시지 | false | 완료 금지, 슬롯 격리, 관리자 안내(6장 참고, 분리 여부 재확인 필요) |
| `WITHDRAWAL_NOT_DETECTED` / `INSERTION_NOT_DETECTED` | Kiosk WS 메시지 | true | 재안내·재검증 |
| `INSPECTION_FAILED` | Kiosk WS 메시지 | false | 물리 반납 계속, 관리자 확인 |
| `RETURN_ALREADY_COMPLETED` / `RETURN_RECOVERY_REQUIRED` | Kiosk WS 메시지 | false | 중복 완료 금지 / 성공 표시 금지, 상태 재조회 |
| `IDEMPOTENCY_CONFLICT` | HTTP 409(App/Admin) 또는 WS 메시지(Kiosk) | false | 같은 키에 다른 payload 사용 |
| `PAYMENT_AMOUNT_MISMATCH` / `PAYMENT_ALREADY_COMPLETED` / `PAYMENT_PROVIDER_ERROR` | HTTP(App) | 상이 | 결제 관련(변경 없음) |

`FACE_ALIGNMENT_REQUIRED`는 오류 코드가 아니다 — `AUTH_PROCESSING` + `faceGuidance` 정상 안내 상태로 취급(변경 없음).

**챗봇 전용 오류(변경 없음)**: `CHATBOT_MESSAGE_REQUIRED`(400), `CHATBOT_MESSAGE_TOO_LONG`(422), `CHATBOT_UNSAFE_INPUT`(422), `CHATBOT_RATE_LIMITED`(429), `CHATBOT_PROVIDER_UNAVAILABLE`(503), `CHATBOT_PROVIDER_TIMEOUT`(504), `CHATBOT_RESPONSE_FAILED`(500).

공통 원칙: 내부 예외 메시지·SQL·스택트레이스·얼굴 유사도 원시값·`paymentKey`·장치 비밀값·MQTT Topic/Client 인증서·WebSocket 자격 증명은 절대 화면·로그·Frontend bundle·환경 변수에 노출하지 않는다(06번 §7에서 WebSocket 자격 증명 항목 추가 확인). 알 수 없는 상태를 성공으로 임의 변환하지 않는다.

---

## 18. 챗봇(`CHATBOT-001`) 상세 — P0 (변경 없음, 06번 §3.2에서 재확인)

- App은 Spring만 호출, 외부 LLM SDK/Key/Endpoint를 앱에 두지 않는다.
- CTA는 기존 App 화면 또는 기존 P0 기능으로 이동하는 링크이며 대여·반납·결제·슬롯 상태를 직접 변경하지 않는다.
- 대화는 현재 화면 메모리에만 두고 화면·앱 종료 시 폐기한다. 대화 테이블·이력 조회/삭제 API 자체를 만들지 않는다.
- 민감정보(비밀번호, 토큰, `paymentKey`, 얼굴 이미지/임베딩, 대학 계정 식별자 원문, 카드번호 등)는 message/context/로그 어디에도 넣지 않는다.
- 외부 LLM 장애·Timeout → 정적 FAQ 즉시 Fallback(`fallback=true`), Fallback도 실패해야 `503/504` 반환.
- 지원 Intent: `RENTAL_GUIDE`, `RETURN_GUIDE`, `EXTENSION_GUIDE`, `LOSS_OR_DAMAGE_GUIDE`, `UNSETTLED_GUIDE`, `STATION_GUIDE`, `GENERAL_INQUIRY`
- 허용 CTA: `OPEN_MAP | OPEN_MY_PAGE | OPEN_HISTORY | OPEN_INQUIRY | NONE`

---

## 19. 변경 이력

| 일자 | 내용 |
|---|---|
| 2026-07-27 | 최초 작성 (v1.0, 00·06번 문서 기준) |
| 2026-07-27 | 08(ERD)·10(API) v1.0 문서 확인 반영, 개발 일정 리스크 반영 개정 |
| 2026-07-27 | 리스크·공백 종합, 디자인 시스템, 개발환경, AI·개인정보 판단근거, 오류코드, `dueAt` 모순·얼굴등록 API 공백·enum 분리·Toss 딥링크 반영 |
| 2026-07-27 | 키오스크 UI 디자인 PDF 실측(픽셀 샘플링)으로 컬러 팔레트 전면 갱신 |
| 2026-07-29 | `DEC-032`(프로젝트 로컬 결정) 추가: 얼굴 인증 가이드 화면 "준비되었습니다" 클릭 시점에 한해 Kiosk FE → Raspberry Pi 직접 WebSocket 연결을 다시 허용, `DEC-024`를 이 트리거 1곳에 한해 번복. 00번 공식 변경이력 출처 아님. 3장·9.1절에 반영. |
| 2026-07-28 | **v2.0으로 전면 갱신·v1.0 지침서와 병합.** 00·06·09·10·12 원문 확인 후 반영, 팀 확인에 따라 v2.0을 새 기준선으로 채택. |
| **2026-07-30** | **v3.0 부분 갱신.** Notion "06. 빌리산 프론트엔드 역할 명세서 v3.0" 전문(95,734자)과 "09. Kiosk·App 화면 흐름 명세 v3.0" 전문(130,123자)을 python 슬라이싱으로 전량 확인 후 반영. 06번·09번 외 문서(00~05, 07~08, 10~12, 12-R, 12-I)는 이번 작업에서 원문을 읽지 않았으며, v3.0을 프로젝트의 새 기준선으로 팀이 채택했다는 절차는 아직 없다 — 이 갱신은 06번·09번 확인 결과를 정확히 반영하기 위한 초안이다. 핵심 변경: (1) **Kiosk↔Pi 통신이 REST에서 다시 WebSocket으로 전환 확정**(`DEC-024` SUPERSEDED, `DEC-049`가 새 근거) — 기존 7개 `KSK-*` ID는 유지되나 HTTP Endpoint가 아니라 Pi WebSocket 논리 Operation으로 재정의됨, (2) 로컬 `DEC-032`(얼굴 인증 트리거 1곳 한정 WebSocket 예외)가 방향은 맞았으나 적용 범위 서술이 06번의 "7개 전체"보다 좁다는 점을 확인 — `DEC-032` 서술 갱신 필요 항목으로 등록, (3) ACK와 최종 Result/Terminal 구분, `progressMode=INDETERMINATE`+0~90% capped animation, `clientSessionKey`(Web Crypto, sessionStorage 한정) 등 WebSocket 수명주기 세부 규칙 신규 반영, (4) `KIOSK_WEBSOCKET_UNAVAILABLE`/`MQTT_BROKER_UNAVAILABLE`/`ORIN_WEBSOCKET_UNAVAILABLE` 오류 코드 3종 추가, (5) 문서 체계상 12-R(REST 23개)·12-I(Kiosk WS 7 + Spring-Pi MQTT 10 + Pi-Orin WS) 신설 확인(단 두 문서 본문은 미확인, mention-page 인용과 Kiosk WS 7개 목록만 확인), (6) 키오스크 폼팩터 "터치 입력 지원 노트북 화면"으로 확정되어 기존 재확인 필요 항목 해소, (7) `DEC-048`(신원 모델)·`DEC-050`(데이터·무결성)은 사용자 제공 배경 정보로만 알고 있으며 06번·09번 원문엔 없어 미확정 표시 유지, (8) 06번 문서 내부에 ARCHIVED(구 v2.1) 서술과 활성 v3.0 서술이 일부 상충(세션 상태 전달 방식 등)하는 점을 리스크로 등록, (9) 09번 v3.0 확인 결과 `docs/09-screen-flow.md`도 같은 날 별도로 전면 갱신함 — §0 문서 목록의 09번 확인 상태를 갱신. 04/05/07/08/10/11/12/12-R/12-I는 여전히 원문 미확인 — 특히 08번(ERD)과 12-I(Kiosk WS 계약 상세)를 다음 우선 확인 대상으로 지정. |
| **2026-07-30(추가)** | **08번 "Billisan ERD 및 데이터 아키텍처 문서 v3.0" 전문(138,145자) 확인 후 갱신.** python 슬라이싱으로 전량 확인(2개 구간). 핵심 반영: (1) `DEC-048`(9자리 학번 `user_id` PK + `user_ref UUID UNIQUE` 가명키, PK 아님)과 `DEC-050`(`V18_SCHEMA_INTEGRITY_BASELINE`: MySQL 업무 11개+내부 Guard 1개=물리 12개, Trigger 9개 명칭 확인, Orin PostgreSQL 업무 1개, Pi 영속 DB 0개)이 모두 **08번 원문에서 확인됨**으로 상태 전환(9.1·9.2절), (2) §12.1 MySQL 테이블 목록을 9개에서 11개 업무+1개 내부 Guard=12개로 정정(`ADMIN_ACCOUNT`, `FACE_PROFILE_SYNC_OPERATION`, `SETTLEMENT_PAYMENT_MUTATION_GUARD` 3개 추가 확인, 13.1절에 리스크로 등록), (3) `FACE_PROFILE`(Orin) PK가 `user_ref UUID` 자체임을 확인, `USER_ACCOUNT`의 `login_id`·`password_hash`·`user_id CHAR(9)` PK·`student_number` 미사용을 모두 확인(3.2·3.3절), (4) `DEVICE_OPERATION`이 `request_id`/`rental_id`/`return_attempt_id` 컬럼 없이 `command_id`/`event_id`만으로 상관하는 독립 장치 원장임을 확인, (5) `STATION.station_id`가 권위 식별자이고 `station_code`·`location_text`는 P0 필수 컬럼이 아님을 확인, (6) 얼굴 등록·인증 네트워크 계층에서 UUID를 직접 전달하지 않고 `profileTicket`/`faceMatchToken`/`faceMatchEvidence`/`authGrant`를 사용한다는 08번 §2.3·§19 신규 확인 사항을 3.2절에 반영. **08번 본문에도 없는 것**: V1~V18 개별 마이그레이션의 버전별 상세 내역(첨부 SQL 파일 자체에만 있음, 08번 텍스트에는 `DEC-048=V13`·`DEC-050=V18` 두 지점만 명시)과 12-I의 WebSocket Heartbeat/Timeout 수치 — 이 두 가지는 계속 미확인으로 명시 유지(9.2·11·13.3절). |
| **2026-07-30(같은 날 추가 갱신)** | **12-I "Billisan 장치 메시지·내부 인터페이스 계약 DB v3.0"의 Kiosk↔Pi WebSocket 7개 Operation 개별 페이지 전체(`KSK-STATION-001, KSK-SESSION-001, KSK-AUTH-001, KSK-SESSION-002, KSK-RENT-001, KSK-RETURN-001, KSK-CANCEL-001`)와 상위 공통 문서("12-I. Billisan 장치 메시지·내부 인터페이스 명세서 v3.0 — P0 구현 기준", 75,727자, notion-fetch + python 슬라이싱 전량 확인)를 조회해 §3.1·§5.4.1(신설)·§9.2·§11·§13.3~13.4·§0 문서표를 갱신했다.** 핵심 변경: (1) **Heartbeat 수치 신규 확인** — WebSocket Ping/Pong 10초 간격, 30초 무응답 시 해당 연결만 `KIOSK_WEBSOCKET_UNAVAILABLE` 판정 후 재연결(12-I 공통 문서 K-0절). 이전까지 "12-I 계약을 따른다"고만 서술했던 항목 해소. (2) **재연결 최대 횟수·지수 Backoff 계수·Jitter 범위는 이번 확인으로도 발견되지 않음** — 12-I 공통 문서·7개 Operation 페이지 전체를 검색했으나 수치 자체가 없다. 계속 미확인 유지, 임의 수치 하드코딩 금지. (3) 공통 Message Envelope 확정: `schemaVersion,messageType,messageId,operationId,sessionId,requestId,correlationId,sentAt,expiresAt,sequence,payload`(K-1절). 공통 ACK Payload(`ackStatus,acceptedAt,errorCode`, ACK Timeout 전 Operation 공통 2초), 공통 Error(`KIOSK.OPERATION.ERROR`, Payload `code,messageCode,retryable,details`)도 확정(K-2절). (4) Operation별 `messageType` 명명 규칙 확정: `KIOSK.<도메인>.<REQUEST|ACK|EVENT|RESULT>` 패턴(예: `KIOSK.STATION_SUMMARY.REQUEST`, `KIOSK.FACE_AUTH.REQUEST`, `KIOSK.RENT.REQUEST`, `KIOSK.RETURN.REQUEST`, `KIOSK.SESSION.OPEN/STATE/CANCEL.REQUEST`). (5) §5.4.1을 신설해 7개 Operation의 messageType·핵심 Request/Result 필드·주요 Error Code·Timeout을 표로 정리(상세는 `docs/09-screen-flow.md` 3.1~3.6절에도 화면별로 반영). (6) **미확인으로 새로 확인된 공백**: `ACTIVE_RENTAL_EXISTS`/`ACTIVE_RENTAL_NOT_FOUND`/`UNSETTLED_EXISTS` 오류 코드가 어느 Operation·어느 필드에서 오는지 12-I 개별 페이지 어디에도 명시적으로 없음 — 추측하지 않고 미확인 유지. (7) `KSK-DIAG-001`/`KSK-FAN-001`, Spring↔Pi MQTT 10개, Pi↔Orin WebSocket 상세는 이번 확인 범위 밖으로 그대로 미확인. |
| **2026-07-30(병합)** | **08번 ERD 확인 초안과 12-I 확인 초안, 두 개의 독립적인 전체 교체 초안을 하나로 병합해 최종 반영.** 두 초안은 같은 원본(06+09번 확인 완료 상태)에서 출발해 서로 다른 문서(08번 ERD 전문·12-I Kiosk WS 7개 Operation 전문)를 각각 추가로 확인했으며, 겹치지 않는 섹션이 대부분이었다. 병합 원칙: 정보를 삭제하지 않고 두 초안 중 한쪽에만 있는 확인 내용은 전부 반영, 같은 섹션에 서로 다른 내용이 추가된 경우(§9.2, §11, §13.3, §13.4, 변경 이력)는 합쳐서 반영, 아직 실제로 미확인인 항목("미확인" 표시)은 그대로 유지했다. 섹션별 채택: §3.1(Kiosk WebSocket 연결·메시지 수명주기, Heartbeat·Envelope·messageType 상세)은 12-I 초안 채택, §3.2(얼굴 등록 이미지 흐름, `FACE_PROFILE` PK·Token/Evidence 체계)·§3.3(인증·계정 모델, `DEC-048` 상세 구조)·"데이터·통신 기준"(`FACE_PROFILE` 컬럼 상세)은 08번 ERD 초안 채택, §5.4(신규 §5.4.1 "Operation별 메시지 스키마 상세" 표)는 12-I 초안 채택 후 기존 §5.4 본문(반납 화면 단계·대여 흐름·보류 판단)을 그대로 이어 붙임, §9.1·§12.1·§13.1(항목 8 추가)은 08번 ERD 초안 채택, §9.2·§11·§13.3·§13.4는 두 초안의 갱신 내용을 모두 반영해 병합(예: `DEC-048`/`DEC-050`은 08번 확인으로 해소, Heartbeat 수치는 12-I 확인으로 해소, 재연결 횟수·Backoff 계수·Jitter 범위·12-R 본문 상세는 계속 미확인으로 유지). 헤더·빠른 참조·§0 문서 목록 표는 06·09·08번 전문 확인 + 12-I 부분 확인(Kiosk WS 7개 Operation) 상태를 모두 반영하도록 갱신했다. 변경 이력은 하나로 합치지 않고 06+09번 항목 → 08번 항목 → 12-I 항목 → 이번 병합 항목 순서로 시간순 나열을 유지했다. |

**절대 기준선**: `Kiosk FE ↔ Pi WebSocket 7개 논리 Operation`(`KSK-STATION-001, KSK-SESSION-001, KSK-AUTH-001, KSK-SESSION-002, KSK-RENT-001, KSK-RETURN-001, KSK-CANCEL-001`), `Spring↔Pi MQTT(EC2 Mosquitto)`, `Pi↔Orin WebSocket` 통신 경계를 유지한다. Kiosk FE는 Spring·MQTT·DB·GPIO·Orin을 직접 호출하지 않는다.
