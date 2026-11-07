# 기존 이력 매핑·이전

## 신규 설치

V1~V11이 순서대로 실행된다. V11은 기존 이름과 충돌하는 세 테이블을 아래처럼 보존하고 새 모델을 만든다.

| 구버전 | 보존 이름 |
| --- | --- |
| rental_contract | legacy_rental_contract |
| condition_record | legacy_condition_record |
| return_inspection | legacy_return_inspection |

member_account, rental_product, rental_location, product_category, condition_image, damage_record 및 기존 예측 테이블도 삭제하지 않는다. 기존 FK는 MySQL의 테이블 이름 변경을 따라 기존 이력을 참조한다.

## 기존 데이터가 있는 설치

V11은 기존 데이터 보존·새 스키마 생성, V12는 이전 대응표와 실행 기록을 추가한다. 실제 이력 복사는 관리자가 매핑을 입력해 사전 점검을 통과한 뒤 실행한다. 앱 시작만으로 기존 이력을 복사하지 않는다.

1. 기존 DB·사진 원본 백업과 쓰기 중단 시점 확정. 진행 중 대여 계약 목록 별도 확인.
2. 상품별 실제 소유 사업자 제공. 지점과 사업자를 임의로 1:1 대응시키지 않음.
3. 기존 회원의 신규 login_id와 현재 계정 연결 여부 확정. 신규 이전 계정은 비밀번호 해시·계정 상태를 보존. USER→CUSTOMER, OPERATOR→INSPECTOR. ADMIN으로 자동 승격하지 않음.
4. 상품 분류, 기존 APPROVED/PICKED_UP/IN_USE 등의 계약 상태와 새 상태의 매핑 확정.
5. 기존 BEFORE/AFTER 기록을 INTAKE/RETURN의 차수로 배치. 확정 여부·검수 기준 기록을 업무 이력으로 확인.
6. 사진 파일 존재·메타데이터·SHA-256 점검. 손상 관찰·기존 매칭 결과를 새 판정에 연결할 대응표 작성.
7. 건수·원본 ID 대응표·FK·사진 조회·진행 중 계약을 검증한 후 새 API로 전환.

구버전 회원 ID와 새 회원 ID가 같아도 기존 JWT를 재사용할 수 없도록 issuer를 변경했다. 인증 정보 이전 후에도 다시 로그인해야 한다.

## 적용 중 실패·복구

MySQL DDL은 마이그레이션 전체가 하나의 트랜잭션으로 롤백되지 않는다. V11 중간 실패 시 자동 재시도·Flyway repair부터 실행하지 않는다. 백업과 실제 생성·변경된 테이블, Flyway 이력을 대조하고 복구 절차를 정한다.

## 관리자 화면에서 실행

1. 실제 소유 사업자를 사업자 등록 화면에 먼저 등록.
2. **기존 이력 이전**에서 원본 목록과 사업자·검수 담당자 ID 확인.
3. 생성된 매핑 JSON을 편집. products의 businessId·categoryCd, inspections의 실제 담당자를 입력. 필요하면 회원 기존 계정 연결·로그인 ID·자산 번호·비표준 손상 위치 매핑을 지정.
4. **사전 점검**으로 오류·확인 사항·대상 건수 확인. 오류가 있으면 이전 버튼 비활성화.
5. **점검한 매핑으로 이전** 실행. 계약·사진·검수 등이 새 회원의 대여 이력에 연결됨.

형식 예시 — 숫자는 실제 대응표로 교체:

```json
{
  "members":[{"legacyMemberId":1,"loginId":"legacy-1","existingMemberId":null}],
  "products":[{"legacyProductId":10,"businessId":3,"categoryCd":"CAMERA","assetCode":null}],
  "inspections":[{"legacyInspectionId":20,"inspectorMemberId":null,"legacyInspectorMemberId":2}],
  "damageLocations":[{"source":"UPPER_LEFT_CORNER","target":"EDGE"}]
}
```

- products를 선택하면 그 상품의 모든 계약·상태·사진·관찰·검수를 함께 이전. 계약 소유 회원과 지정한 기존 검수 담당자도 포함.
- members만 지정해 회원부터 이전하는 것도 가능. 기본 login_id는 `legacy-{원본 ID}`. members에서 별도 지정 가능.
- existingMemberId는 동일 이메일의 실제 본인 계정을 관리자가 확인해 연결할 때만 사용. 현재 계정의 이름·로그인·비밀번호·역할·상태를 덮어쓰지 않음.
- 상품은 기존 실물 자산별로 새 ID를 만들고 사업자에 연결. 지점을 사업자로 추정하지 않음. 자산 번호가 40자를 넘으면 assetCode에 명시적인 새 식별자 지정. 원본은 보존.
- 검수 담당자는 원본에 없으므로 inspectorMemberId(현재 담당자) 또는 legacyInspectorMemberId(기존 OPERATOR) 중 하나를 실제 자료로 지정. 이전 실행자를 과거 담당자로 간주하지 않음.
- categoryCd: CAMERA / COMPUTER / HOME_APPLIANCE / SPORTS. 의미가 다른 기존 상품 분류를 자동 추정하지 않음.
- 원본에 비밀번호가 없으면 NULL 유지. 기존 비밀번호가 있는 계정은 같은 비밀번호로 새 login_id 로그인 가능. 비밀번호가 없는 계정은 본인 확인 후 기존 로그인 계정에 명시적으로 연결하거나 별도 비밀번호 등록 절차가 필요하며, 임의 비밀번호를 생성하지 않음.

## 상태 변환

| 기존 계약 | 새 계약 |
| --- | --- |
| REQUESTED / APPROVED / PICKED_UP | REQUESTED |
| IN_USE | RENTED |
| RETURNED / INSPECTING | RETURNED |
| COMPLETED | INSPECTED |
| REJECTED / CANCELED | CANCELED |

승인·인수·거절 등 기존 세부 단계와 시각은 원본 테이블에 보존하고 상세 응답의 legacySource로 제공. 화면에 원본 단계도 표시한다.

| 기존 상품 | 새 상품 |
| --- | --- |
| AVAILABLE / RESERVED | AVAILABLE |
| RENTED | RENTED |
| INSPECTION | INSPECTION |
| MAINTENANCE | REPAIR |
| RETIRED | INACTIVE |

신청 중에는 REQUESTED 계약이 중복 신청을 차단. 여러 활성 계약 또는 상품·계약 상태가 서로 맞지 않으면 이전 차단.

## 상태 기록·사진·판정

- BEFORE_RENTAL→INTAKE, AFTER_RENTAL→RETURN, 최초 차수 1.
- 이용 시작 이후 사용된 인수 기록을 확정 상태로 이전. 반납 기록은 기존 검수 진행·완료 이후면 확정. 사용된 기준 기록·사진이 없으면 차단.
- 사진 저장 키를 유지하고 현재 ImageStorage에서 파일을 읽어 실제 크기·형식 헤더·SHA-256 계산. 파일 누락이면 해당 계획 전체를 차단. 원래 촬영 방향이 새 코드에 없으면 OTHER로 옮기고 원본 방향은 legacySource로 제공.
- 기존 손상의 위치는 일치하는 코드만 사용하고 나머지는 damageLocations로 명시 매핑. MISSING_PART는 OTHER로 옮기고 원래 손상 유형·위치·비교 결과는 원본과 legacySource에 보존.
- 기존에는 손상별 사진 연결이 없으므로 증거 연결·영역 좌표를 생성하지 않음. 상태별 기존 사진은 표시.
- EXISTING/NEW 판정을 기존 매칭 FK에 따라 복사. 판정 사유는 원본에 없으므로 NULL. 관찰 설명을 판정 사유로 바꾸지 않음.
- REVIEW_REQUIRED는 IN_PROGRESS와 보류를 유지하고 완료 시각을 만들지 않음. 원본의 검수 기록 시각은 별도 메타데이터로 보존.
- 완료된 검수의 결과와 손상 비교가 서로 다르거나 기준 기록·담당자가 없으면 차단.

## 실행 일관성과 재실행

- GET `/api/v2/admin/legacy-import/source`: 관리자 원본 목록·상태 대응표.
- POST `/preview`: 매핑 JSON. 오류·확인 사항·건수·planToken 반환.
- POST `/apply`: `{manifest,planToken}`. ADMIN만 실행 가능.
- 계획 토큰은 원본·대상·매핑·사진 해시를 포함. 점검 뒤 달라지면 새 점검 요구.
- 이전은 전용 잠금으로 직렬화하고 원본 행을 읽기 잠금. 복사·FK 연결·ID 대응표·실행 기록은 한 트랜잭션으로 처리. 중간 DB 실패 시 모두 롤백.
- 같은 토큰·매핑의 재시도는 ALREADY_IMPORTED와 기존 실행 ID 반환. 이미 이전한 새 데이터의 이후 변경을 덮어쓰지 않음.
- 기존 원본 변경이나 뒤늦은 계약·기록 추가는 차단하고 별도 조정 요구. 이전 이후에도 기존 쓰기를 계속하는 동기화 기능이 아님.
- 원본 테이블·사진 파일을 보존하고 이전 기간의 쓰기를 중단해야 함. 실제 자료가 없는 현재 작업에서는 fixture로만 검증했으며 운영 이력을 이전한 결과가 아님.

## 실제 적용 시 필요한 자료

- 기존 DB 백업 또는 접근할 DB 정보.
- 기존 사진 저장소의 실제 파일과 저장 루트.
- 상품별 실제 소유 사업자·등록번호·사업자명 및 새 분류.
- 각 기존 검수의 실제 담당자.
- 충돌 계정의 본인 연결 결정, 비표준 손상 위치 매핑.

## 검증한 범위

- 빈 DB: V1~V11 적용 + Hibernate 스키마 검증.
- 기존 V10 fixture: 회원·상품·계약·인수 기록·사진을 넣은 뒤 V11 적용. 기존 값과 계약→상태→사진 FK 연결 보존 확인.
- 완료·보류·모든 기존 계약 상태의 이전, 기존 비밀번호 로그인·본인 이력·사진 조회, 계정 명시 연결, 파일 누락·변경 감지, 재시도·전체 롤백·권한 검증.
