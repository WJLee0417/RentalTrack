# V11 이전과 보존 범위

## 신규 설치

V1~V11이 순서대로 실행된다. V11은 기존 이름과 충돌하는 세 테이블을 아래처럼 보존하고 새 모델을 만든다.

| 구버전 | 보존 이름 |
| --- | --- |
| rental_contract | legacy_rental_contract |
| condition_record | legacy_condition_record |
| return_inspection | legacy_return_inspection |

member_account, rental_product, rental_location, product_category, condition_image, damage_record 및 기존 예측 테이블도 삭제하지 않는다. 기존 FK는 MySQL의 테이블 이름 변경을 따라 기존 이력을 참조한다.

## 기존 데이터가 있는 설치

**V11은 기존 데이터의 보존과 새 스키마 생성을 수행한다. 새 화면으로의 데이터 이전을 수행하지 않는다.** 기존 운영 DB에는 아래 매핑과 백업·복구 절차를 확정한 뒤 적용한다. 새 API는 새 테이블만 조회하므로 구버전 이력은 자동으로 표시되지 않는다.

1. 기존 DB·사진 원본 백업과 쓰기 중단 시점 확정. 진행 중 대여 계약 목록 별도 확인.
2. 상품별 실제 소유 사업자 제공. 지점과 사업자를 임의로 1:1 대응시키지 않음.
3. 기존 회원의 신규 login_id·역할·상태 매핑 확정. 비밀번호 해시는 member_credential로 이전 가능하지만 자동 복사하지 않음.
4. 상품 분류, 기존 APPROVED/PICKED_UP/IN_USE 등의 계약 상태와 새 상태의 매핑 확정.
5. 기존 BEFORE/AFTER 기록을 INTAKE/RETURN의 차수로 배치. 확정 여부·검수 기준 기록을 업무 이력으로 확인.
6. 사진 파일 존재·메타데이터·SHA-256 점검. 손상 관찰·기존 매칭 결과를 새 판정에 연결할 대응표 작성.
7. 건수·원본 ID 대응표·FK·사진 조회·진행 중 계약을 검증한 후 새 API로 전환.

구버전 회원 ID와 새 회원 ID가 같아도 기존 JWT를 재사용할 수 없도록 issuer를 변경했다. 인증 정보 이전 후에도 다시 로그인해야 한다.

## 적용 중 실패·복구

MySQL DDL은 마이그레이션 전체가 하나의 트랜잭션으로 롤백되지 않는다. V11 중간 실패 시 자동 재시도·Flyway repair부터 실행하지 않는다. 백업과 실제 생성·변경된 테이블, Flyway 이력을 대조하고 복구 절차를 정한다.

## 검증한 범위

- 빈 DB: V1~V11 적용 + Hibernate 스키마 검증.
- 기존 V10 fixture: 회원·상품·계약·인수 기록·사진을 넣은 뒤 V11 적용. 기존 값과 계약→상태→사진 FK 연결 보존 확인.
- 실제 운영 데이터의 상태 매핑·사업자 매핑·사진 이전은 별도 자료가 필요함.
