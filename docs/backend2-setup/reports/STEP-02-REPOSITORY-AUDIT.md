# STEP-02 Repository Audit 보고서

## 판정

`PASSED`

저장소 감사 자체는 완료되었다. 다만 현재 작업 브랜치에는 빌드 가능한
Backend 프로젝트가 없으므로, 다음 구현 단계 전에 미병합 Spring Boot
초기화 브랜치의 통합 여부와 DB 기술 선택을 확정해야 한다.

## 감사 범위

- 저장소 루트: `C:\Users\SSAFY\S15P11A201`
- 현재 브랜치: `S15P11A201-55-be-db`
- 현재 커밋: `ed68f97451b1327ea666bb3ba9732bfce84bd08b`
- 기준 브랜치: `origin/develop`
- 비교 후보: `origin/S15P11A201-50-be-springboot-setting`
- 제외 범위: 사용자 지시에 따라 `origin/subPjt_1` 기술조사 문서는
  근거와 결론에서 제외

현재 `HEAD`, `origin/develop`, `origin/S15P11A201-55-be-db`는 같은
커밋이다. `git ls-files`로 확인한 현재 트리의 추적 파일은 다음 7개다.

```text
README.md
ai/.gitkeep
backend/.gitkeep
frontend/app/.gitkeep
frontend/kiosk/.gitkeep
frontend/web/.gitkeep
iot/.gitkeep
```

`README.md` 내용은 프로젝트 제목 `# S15P11A201`뿐이다.

## 필수 판정표

| 항목 | 판정 | 실제 근거 |
| --- | --- | --- |
| MySQL Driver | 현재 브랜치와 미병합 초기화 후보에서 확인되지 않음 | 현재 트리 전체 `git grep` 무결과; 후보 `backend/build.gradle`에는 기본 starter/test starter만 선언 |
| ORM 또는 SQL Mapper | JPA/Hibernate/MyBatis 선택 미확정 | 현재 트리 전체 키워드 검색 무결과; 후보 `build.gradle`에도 관련 의존성 없음 |
| Migration 도구 | Flyway/Liquibase 모두 미도입 또는 미확정 | 현재 트리 파일명·내용 검색 무결과; 후보 트리에도 Migration 파일과 의존성 없음 |
| Migration 실행 주체 | 미확정 | Migration 도구, 스크립트, Compose 초기화 디렉터리가 없음 |
| MySQL Compose 존재 여부 | 현재 및 후보 트리에서 확인되지 않음 | Dockerfile/`compose*.yml`/`docker-compose*.yml` 파일명 검색 무결과 |
| PostgreSQL+pgvector Compose 존재 여부 | 현재 및 후보 트리에서 확인되지 않음 | Compose 파일 없음; 현재 트리 DB 키워드 검색 무결과 |
| Spring의 Orin DB 직접 연결 여부 | 직접 연결 근거를 확인할 수 없음; 정책 미확정 | 현재 브랜치에 Spring 소스와 `application*`이 없고 후보 `application.yaml`에는 애플리케이션 이름만 있음 |
| Device Gateway 패키지 위치 | 현재 및 후보 트리에 없음; 위치 미확정 | Java/Kotlin 파일명 및 `Device`, `Gateway`, `gateway` 내용 검색 무결과 |
| 장치 DTO 패키지 위치 | 현재 및 후보 트리에 없음; 위치 미확정 | `dto`, `request`, `response` 경로와 Java/Kotlin 소스 검색 무결과 |
| 동시성 제어 방식 | 구현과 팀 기준 모두 미확정 | `@Version`, pessimistic/optimistic lock, `FOR UPDATE`, Java lock 및 동시성 키워드 검색 무결과 |
| 테스트 DB 방식 | 미확정 | 테스트 프로파일, DB 의존성, Testcontainers가 없음; 후보에는 DB 없는 `contextLoads()`만 있음 |
| 빌드 명령 | 현재 브랜치에는 실행 가능한 명령 없음 | Gradle/Maven 빌드 파일과 Wrapper가 현재 트리에 없음 |
| 테스트 명령 | 현재 브랜치에는 실행 가능한 명령 없음 | 빌드 진입점과 테스트 소스가 현재 트리에 없음 |
| 확정되지 않은 팀 결정 | 초기화 브랜치 통합, 버전 승인, DB/ORM/Migration/Compose/DataSource/Gateway/동시성/테스트 전략 | 아래 “미확정 팀 결정” 참조 |

## 조사 항목별 결과

| 번호 | 조사 항목 | 현재 브랜치 결과 | 근거 또는 제한 |
| --- | --- | --- | --- |
| 1 | 프로젝트 루트와 멀티모듈 여부 | Git 루트 아래 작업 영역만 존재. 빌드 멀티모듈 여부 미확정 | `settings.gradle*`, `build.gradle*`, `pom.xml` 없음 |
| 2 | Spring Boot 버전 | 미확정 | 현재 브랜치에 Backend 프로젝트 없음 |
| 3 | Java Toolchain/JDK | 저장소 기준 미확정; 로컬 JDK는 Temurin 21 | STEP-01 도구 탐지. 후보 브랜치는 toolchain 21 |
| 4 | Gradle DSL | 현재 기준 미확정 | 후보 브랜치는 `build.gradle`, `settings.gradle` Groovy DSL |
| 5 | Gradle Wrapper | 현재 브랜치에 없음 | `gradlew*`, wrapper properties 검색 무결과 |
| 6 | MySQL Driver | 확인되지 않음 | 현재 전체 추적 트리와 후보 `build.gradle` 점검 |
| 7 | JPA/Hibernate/MyBatis | 확인되지 않음 | 의존성·import·설정·소스가 없음 |
| 8 | Flyway/Liquibase | 확인되지 않음 | 의존성 및 Migration 경로/파일 없음 |
| 9 | Testcontainers | 확인되지 않음 | 의존성 및 테스트 소스 없음 |
| 10 | `application*` 위치 | 현재 브랜치에 없음 | 후보에만 `backend/src/main/resources/application.yaml` |
| 11 | Dockerfile/Compose/개발 스크립트 | 확인되지 않음 | 전체 파일명 검색 무결과 |
| 12 | 기존 MySQL 실행 구성 | 확인할 구성 없음 | Driver, DataSource, Compose, 스크립트 없음 |
| 13 | 기존 PostgreSQL+pgvector 실행 구성 | 확인할 구성 없음 | Driver, DataSource, Compose, 스크립트 없음 |
| 14 | Migration 위치/버전 규칙 | 미확정 | SQL, `db/migration`, changelog 경로 없음 |
| 15 | Entity/Repository/Mapper 위치 | 미확정 | 관련 경로·소스 없음 |
| 16 | Device Gateway 위치 | 미확정 | 관련 경로·소스 없음 |
| 17 | 장치 요청·응답 DTO 위치 | 미확정 | 관련 경로·소스 없음 |
| 18 | 요청/명령/이벤트/상태 코드 | 확인되지 않음 | `requestId`, `returnAttemptId`, `commandId`, `eventId`, `heartbeat`, `snapshot` 검색 무결과 |
| 19 | 슬롯 동시성 제어 | 미확정 | 슬롯 도메인과 동시성 코드가 모두 없음 |
| 20 | Spring의 Orin용 두 번째 DataSource | 확인되지 않음 | Spring 설정이 없고 후보 설정에도 `spring.application.name`만 있음 |
| 21 | 잘못된 저장 흔적 | 현재 범위에서 확인되지 않음 | Redis, SQLite, 얼굴/이미지/임베딩 키워드와 저장 코드가 모두 없으므로 저장 정책 자체도 감사 불가 |
| 22 | 테스트 프로파일/통합 테스트 | 현재 브랜치에 없음 | 후보에는 프로파일 없는 `@SpringBootTest contextLoads()`만 있음 |
| 23 | 현재 빌드·테스트 명령 | 없음 | 현재 브랜치에 Gradle/Maven 진입점 없음 |
| 24 | 문서의 DB 버전 | MySQL/PostgreSQL/pgvector 모두 미기재 | 현재 브랜치 `README.md`와 STEP-01 문서 기준. 제외된 `subPjt_1` 문서는 근거로 사용하지 않음 |

`확인되지 않음`은 현재 브랜치의 전체 Git 추적 트리와 명시한 미병합
초기화 후보를 대상으로 한 검색 결과다. 기능이 다른 브랜치, 외부 문서,
로컬 미추적 파일 또는 향후 커밋에 존재하지 않는다고 단정하지 않는다.

## 현재 기준선과 미병합 후보

### 현재 확정 기준선

- 현재 작업 브랜치와 `origin/develop`은 동일한 placeholder 트리다.
- `backend/.gitkeep`만 있으며 Backend 애플리케이션은 아직 없다.
- 따라서 Spring Boot/Gradle/DB/패키지/테스트 기준은 현재 브랜치에서
  확정할 수 없다.

### 미병합 초기화 후보

`origin/S15P11A201-50-be-springboot-setting`은 `origin/develop`보다
1커밋 앞선 별도 브랜치이며 아직 병합되지 않았다.

| 후보 항목 | 값 | Git object 근거 |
| --- | --- | --- |
| 프로젝트 위치 | `backend` | 후보 트리 |
| Spring Boot | `4.1.0` | `backend/build.gradle` |
| Java Toolchain | `21` | `backend/build.gradle` |
| Gradle DSL | Groovy | `build.gradle`, `settings.gradle` |
| Gradle Wrapper | 존재 | `gradlew`, `gradlew.bat`, wrapper 파일 |
| Gradle 배포 버전 | `9.5.1` | `gradle/wrapper/gradle-wrapper.properties` |
| 기본 패키지 | `com.ssafy.billisan` | `BillisanApplication.java` |
| 설정 | 애플리케이션 이름만 지정 | `src/main/resources/application.yaml` |
| 테스트 | `@SpringBootTest` `contextLoads()` | `BillisanApplicationTests.java` |
| DB 관련 | 없음 | 후보 `build.gradle`과 전체 후보 트리 |

후보 파일은 현재 작업 트리에 존재하지 않으므로 로컬 파일 경로처럼
취급하지 않고 `origin/S15P11A201-50-be-springboot-setting:<path>`
형식의 Git object 근거로만 사용했다.

## 기존 항목과 도입 필요 항목

### 현재 이미 존재

- Git 저장소의 영역 구조: `ai`, `backend`, `frontend`, `iot`
- 접두사 없는 작업 브랜치 규칙
- 로컬 Java 21 실행 환경
- 미병합 후보 브랜치의 최소 Spring Boot 스캐폴드

### 다음 변경 전에 통합 또는 도입 필요

- 현재 작업 브랜치에 빌드 가능한 Spring Boot 프로젝트
- 확정된 Gradle Wrapper와 빌드 진입점
- MySQL Driver와 DataSource 설정
- 선택된 ORM 또는 SQL Mapper
- 선택된 Migration 도구, 실행 주체, 위치 및 버전 규칙
- MySQL과 필요한 경우 PostgreSQL+pgvector의 로컬 실행 구성
- Entity/Repository/Mapper 및 Device Gateway/DTO 패키지 구조
- 장치 메시지 식별자·멱등성·동시성 계약
- 테스트 프로파일과 실제 DB 또는 Testcontainers 기반 통합 테스트

## 아키텍처 충돌과 위험

1. **브랜치 기준선 충돌**  
   작업 브랜치 `S15P11A201-55-be-db`는 `develop`과 같은 placeholder
   커밋에서 시작했지만, 선행 성격의 `S15P11A201-50-be-springboot-setting`
   스캐폴드는 아직 병합되지 않았다. DB 작업을 바로 시작하면 같은
   `backend` 경로에 프로젝트 구조를 중복 생성할 위험이 있다.

2. **버전 기준 미승인**  
   미병합 후보는 Spring Boot `4.1.0`, Java `21`, Gradle `9.5.1`을
   제안한다. 현재 `develop` 문서에는 이 버전을 승인하는 기준이 없다.

3. **영속성 기준 공백**  
   MySQL Driver, JPA/MyBatis, Migration 도구, DB 버전, 스키마 실행
   주체가 모두 미확정이다. 임의 도입 시 팀 표준과 충돌할 수 있다.

4. **다중 DB 책임 공백**  
   Orin PostgreSQL/pgvector와 Spring의 직접 연결 또는 두 번째
   DataSource에 대한 저장소 근거가 없다. 연결 경계를 먼저 결정하지
   않으면 서비스 책임과 데이터 소유권이 불명확해진다.

5. **장치 계약과 정합성 공백**  
   Gateway/DTO/식별자/슬롯 동시성 구현이 없어 중복 요청, 재전송,
   경합에 대한 저장소 기준을 확인할 수 없다.

6. **재현 가능한 검증 환경 부재**  
   현재 브랜치에는 Wrapper, Compose, 테스트 프로파일, Testcontainers가
   없어 빌드와 통합 테스트를 재현할 진입점이 없다.

## 미확정 팀 결정

- 선행 Spring Boot 초기화 커밋을 언제 어떤 브랜치 전략으로 통합할지
- Spring Boot `4.1.0`, Java `21`, Gradle `9.5.1` 승인 여부
- 단일 Gradle 프로젝트 또는 저장소 멀티모듈 구성 여부
- MySQL 및 PostgreSQL/pgvector 제품 버전
- JPA/Hibernate와 MyBatis 중 사용할 영속성 접근
- Flyway/Liquibase/별도 운영 도구 중 Migration 방식과 실행 주체
- Migration 파일 위치와 버전 네이밍 규칙
- MySQL 및 PostgreSQL+pgvector Compose 제공 범위
- Orin DB 데이터 소유권과 Spring 직접 연결/두 번째 DataSource 허용 여부
- Entity/Repository/Mapper/Gateway/DTO 패키지 규칙
- `requestId`, `returnAttemptId`, `commandId`, `eventId` 형식과 멱등성 범위
- heartbeat/snapshot 메시지 계약과 보존 정책
- 슬롯 동시성 제어 방식과 DB 제약 조건
- Redis, Pi SQLite, 얼굴 이미지 및 임베딩 저장 위치 정책
- 테스트 DB 방식과 Testcontainers 도입 여부

## 빌드와 테스트 진입점

### 현재 작업 브랜치

빌드와 테스트 명령이 없다. Gradle/Maven 프로젝트 파일, Wrapper 및
테스트 소스가 현재 트리에 없으므로 명령을 실행하지 않았다.

### 미병합 후보가 통합된 이후의 예상 진입점

Windows PowerShell 기준 후보 Wrapper의 실제 파일 위치는
`backend/gradlew.bat`이다.

```powershell
Set-Location backend
.\gradlew.bat build
.\gradlew.bat test
```

이는 현재 브랜치에서 실행 가능한 명령이 아니라, 미병합 후보 파일을
통합했을 때 사용할 수 있는 진입점이다. STEP-02에서는 Checkout, Merge,
애플리케이션 코드 변경 또는 빌드 실행을 수행하지 않았다.

## 검색 방법과 무결과 해석

현재 Git 추적 트리 전체를 대상으로 다음 범주를 파일명과 내용 양쪽에서
검색했다.

- 빌드: `settings.gradle*`, `build.gradle*`, Gradle Wrapper, `pom.xml`,
  Maven Wrapper
- 설정: `application*.yml`, `application*.yaml`,
  `application*.properties`
- 인프라: Dockerfile, Compose, PowerShell/Shell/Batch 스크립트
- Migration: SQL, `db/migration`, changelog, migrations 경로
- 소스: Java/Kotlin 및 Entity/Repository/Mapper/Gateway/DTO 경로
- 기술 키워드: Spring Boot, MySQL, PostgreSQL, pgvector, JPA,
  Hibernate, MyBatis, Flyway, Liquibase, Testcontainers, DataSource,
  Redis, SQLite
- 장치/정합성 키워드: `requestId`, `returnAttemptId`, `commandId`,
  `eventId`, `heartbeat`, `snapshot`, lock, `@Version`, `FOR UPDATE`
- 테스트: 테스트 소스, Spring 테스트 프로파일, 통합 테스트 키워드

현재 추적 트리가 7개 파일뿐이라는 `git ls-files` 결과와 각 범주의
무결과를 함께 근거로 사용했다. 사용자 요청에 따라 `origin/subPjt_1`
기술조사 문서는 검색 결론에 사용하지 않았다.

## 다음 단계에서 수정 가능한 범위

다음 단계는 팀이 승인한 기준선에 따라 다음 중 명시된 범위만 수행해야 한다.

1. 미병합 Spring Boot 초기화 후보의 통합 또는 동등한 스캐폴드 확보
2. 승인된 DB/영속성/Migration 의존성과 설정 추가
3. 승인된 로컬 DB 실행 구성과 테스트 진입점 추가
4. 합의된 패키지·장치 계약·동시성 기준에 따른 최소 구조 추가

미확정 결정을 임의로 선택하거나, 두 번째 DataSource를 발견·도입·제거하는
작업은 별도 승인 없이 수행하지 않는다.

## 다음 실행 프롬프트

`STEP-03`
