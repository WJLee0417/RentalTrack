# RentalTrack

개별 렌탈 상품의 대여·상태 기록·반납 검수 이력을 관리하는 웹 애플리케이션.

- 사업자별 개별 상품 등록, 동일 상품의 중복 대여 신청 방지
- 인수·반납 상태 기록과 사진·손상 관찰·증거 사진 연결
- 상태 기록 확정, 새 차수 작성, 이전 확정 기록 보존
- 선택한 인수·반납 기록을 고정한 검수와 손상별 판정
- 일반회원·검수담당자·관리자 권한, JWT 인증, 사진 조회 권한
- 사진 해시·파일 메타데이터와 이미지 분석 후보 결과 저장

이미지 분석 결과 등록 API는 외부 모델의 결과를 저장한다. 이미지 추론 모델·학습·추론 서버는 이 변경에 포함하지 않는다.

## 구성

| 영역 | 구성 |
| --- | --- |
| Backend | Java 21, Spring Boot, Spring Security, JPA, Flyway |
| Database | MySQL 8.4 |
| Frontend | React, TypeScript, Vite |
| 이미지 | 파일 저장소 + DB 메타데이터·SHA-256 |

새 설계는 [제공된 ERD](docs/design/rentaltrack-v2.damx)를 기반으로 한다. 원본에서 확정하지 못한 코드값·키·제약은 [구현 결정](docs/ERD.md)에 구분해 기록했다.

## 실행

```powershell
docker compose up -d mysql
cd backend
.\gradlew.bat bootRun
```

별도 터미널:

```powershell
cd frontend/web
npm ci
npm run dev
```

- Web: `http://localhost:5173`
- API: `http://localhost:8080`
- 백엔드가 다른 주소라면 프런트엔드 실행 전에 `RENTALTRACK_API_URL` 환경변수 설정.
- 회원가입 시 일반회원으로 생성. 로그인 ID는 영문·숫자·`.`·`_`·`-` 조합 3~50자, 비밀번호는 10~72자.
- 검수담당자·관리자는 DB 관리 권한으로 역할을 부여. 공개 회원가입으로 역할을 지정할 수 없음.

최초 관리자 설정 예시 — 가입한 실제 login_id를 지정:

```sql
UPDATE `member` SET member_role_cd='ADMIN' WHERE login_id='your_admin_login';
```

관리자는 화면에서 사업자·개별 상품을 등록하고, 검수담당자는 계약 ID로 조회해 검수를 진행한다. 역할 변경 후 다시 로그인한다.

## 검증

운영 DB와 분리한 임시 MySQL 사용:

```powershell
docker compose -f compose.test.yaml up -d --wait
$env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:33317/rentaltrack_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul'
cd backend
.\gradlew.bat test bootJar --no-daemon
cd ../frontend/web
npm ci
npm run build
```

업그레이드 테스트는 별도 `rentaltrack_upgrade_test` DB만 초기화한다. 테스트용 compose가 이 DB와 권한을 준비한다. 테스트 종료 후 저장소 루트에서 `docker compose -f compose.test.yaml down` 실행. [검증 항목](docs/VALIDATION.md) 참조.

## 기존 버전에서 변경된 점

- 활성 애플리케이션에서 지점·수요 예측·재고 추천 기능을 제외하고 대여·상태 기록·반납 검수로 재구성.
- 구버전 AI 실험 코드는 `archive/ai/`, 문서는 `docs/legacy/`에 보존. 새 API와 연결되지 않음.
- 인증 경로는 `/api/v1/auth/*`, 새 업무 API는 `/api/v2/*`. 구버전 업무 API는 제공하지 않음.
- V1~V10은 수정하지 않음. V11에서 충돌하는 기존 테이블 세 개를 `legacy_*`로 이름 변경하고 새 모델 생성.
- **기존 이력·회원은 기존 테이블에 보존되며 새 화면으로 자동 이전되지 않음.** 운영 데이터가 있다면 [이전 절차](docs/MIGRATION.md)에 따라 사업자·상품·상태 매핑을 먼저 확정해야 함.

## 문서

- [도메인과 상태 전이](docs/DOMAIN.md)
- [ERD·물리 제약·코드값](docs/ERD.md)
- [API](docs/API.md)
- [DB 이전](docs/MIGRATION.md)
- [검증](docs/VALIDATION.md)
