# STEP-03 Spring Local Baseline 보고서

## 판정

`PASSED`

기존 원격 Spring Boot 초기화 스캐폴드를 재사용해 JDK 21, Gradle
Wrapper, `local` Profile 및 Secret 분리 기준을 구성했다. 컴파일,
기본 테스트, local Profile 컨텍스트 테스트와 전체 build가 성공했다.

## 선행 조건

- STEP-01: `PASSED`
- STEP-02: `PASSED`
- 현재 브랜치: `S15P11A201-55-be-db`
- Commit/Merge/Push/MR: 수행하지 않음

STEP-02에서 확인한
`origin/S15P11A201-50-be-springboot-setting:backend`을 현재
작업 트리에 재사용했다. 새 Spring 프로젝트를 임의 생성하거나 선행
스캐폴드를 중복 작성하지 않았다.

## JDK와 Gradle 기준

| 항목 | 결과 | 근거 |
| --- | --- | --- |
| Java Toolchain | `21`, 기존 값 유지 | `backend/build.gradle` |
| 실행 JDK | Temurin `21.0.11+10-LTS` | `java -version`, `javac -version` |
| Spring Boot | `4.1.0`, 기존 값 유지 | `build.gradle` |
| Gradle DSL | Groovy | `build.gradle`, `settings.gradle` |
| Gradle Wrapper | `9.5.1` | `gradle-wrapper.properties`, Wrapper 실행 결과 |
| Gradle Launcher JVM | `21.0.11` | `gradlew.bat --version` |
| Gradle Daemon JVM | 설치된 JDK 21 | Wrapper 실행 결과 |

저장소 Toolchain과 환경 가이드의 JDK 21 기준이 일치하므로
`build.gradle`의 Java 버전을 변경하지 않았다. Spring Boot, Gradle 및
기타 의존성 업그레이드도 수행하지 않았다.

## JAVA_HOME 환경 이슈

시스템 `JAVA_HOME`은 다음과 같이 존재하지 않는 JRE 경로를 가리켰다.

```text
C:\Users\SSAFY\AppData\Local\Programs\Java\Temurin21\jdk-21.0.11+10-jre
```

반면 `java.exe`와 `javac.exe`는 다음 JDK 21에 설치되어 있었다.

```text
C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
```

빌드 명령 세션에서만 `JAVA_HOME`을 올바른 JDK 경로로 지정했다.
시스템 또는 사용자 환경 변수는 자동 변경하지 않았다.

## Profile 조사와 적용

기존 설정은 `application.yaml`의 애플리케이션 이름뿐이었고, 활성화
Profile이나 Profile별 설정은 없었다. 기존 네이밍과 충돌하지 않으므로
`application-local.yaml`을 최소 추가했다.

```yaml
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: ${MYSQL_URL:jdbc:mysql://localhost:3306/billisan_p0_p1?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC}
    username: ${MYSQL_USERNAME:billisan}
    password: ${MYSQL_PASSWORD}
```

- 비밀번호는 기본값 없이 환경 변수로만 받는다.
- 업무 DB 구성은 MySQL 한 세트뿐이다.
- PostgreSQL URL과 두 번째 DataSource는 추가하지 않았다.
- Pi Base URL, Timeout, Retry, Station ID를 소비하는 코드가 없어
  관련 환경 변수를 추가하지 않았다.

현재 스캐폴드에는 JDBC starter와 MySQL Driver가 없다. STEP-02에서
ORM/SQL Mapper와 DB 의존성 선택이 미확정이므로 이번 단계에서는 이를
임의 도입하지 않았다. 위 설정은 Driver 승인·도입 후 사용할 단일
MySQL DataSource 환경 변수 계약이며, 이번 테스트에서 실제 DB 연결을
시도하지 않는다.

## 환경 변수

`backend/.env.example`에 다음 비민감 예제를 추가했다.

| 변수 | 예시 | 설명 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` | 로컬 Profile 활성화 |
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/billisan_p0_p1?...` | 로컬 MySQL JDBC URL |
| `MYSQL_USERNAME` | `billisan` | 로컬 사용자 예시 |
| `MYSQL_PASSWORD` | `change-me` | 실제 Secret이 아닌 교체용 표시값 |

`.gitignore` 검증 결과:

- `.env`: ignore
- `.env.local`: ignore
- `.env.example`: ignore 예외로 저장소 포함 가능
- `.gradle`, `build`: 기존 규칙으로 ignore

Spring Boot는 `.env.example`을 자동 로드하지 않는다. 실제 개발자는
PowerShell, IntelliJ 실행 구성 또는 별도 승인된 로컬 환경 로더를 통해
환경 변수를 주입해야 한다.

## 실행 명령과 결과

작업 디렉터리:

```powershell
Set-Location C:\Users\SSAFY\S15P11A201\backend
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:GRADLE_USER_HOME = (Join-Path (Get-Location) '.gradle')
```

| 검증 | 명령 | 결과 |
| --- | --- | --- |
| Wrapper 버전 | `.\gradlew.bat --version` | `PASSED` — Gradle 9.5.1, JVM 21.0.11 |
| 컴파일 | `.\gradlew.bat --no-daemon compileJava` | `PASSED` — 1 task executed |
| 기본 테스트 | `.\gradlew.bat --no-daemon test` | `PASSED` |
| local Profile 테스트 | `SPRING_PROFILES_ACTIVE=local`, 임시 `MYSQL_PASSWORD`와 `--rerun-tasks test` | `PASSED` — local Profile 활성 확인 |
| 전체 build | `.\gradlew.bat --no-daemon build` | `PASSED` — 실행 Jar 생성 |

테스트 XML 결과:

```text
tests=1, skipped=0, failures=0, errors=0
contextLoads() PASSED
active profile: local
```

생성된 build 산출물:

```text
build/libs/billisan-0.0.1-SNAPSHOT.jar
build/libs/billisan-0.0.1-SNAPSHOT-plain.jar
```

`build`와 `.gradle`은 Git ignore 대상이며 변경 파일 목록에 포함되지 않는다.

## 실패와 환경 차단 분류

| 항목 | 분류 | 조치 |
| --- | --- | --- |
| 최초 Wrapper 실행 | 환경 설정 차단 | 잘못된 시스템 `JAVA_HOME`을 명령 세션에서 JDK 21로 보정 |
| 최초 Wrapper 다운로드 | Sandbox 네트워크 차단 | 승인된 네트워크 실행으로 Gradle 9.5.1 다운로드 |
| local 테스트 최초 강제 재실행 | Sandbox/캐시 파일 권한 차단 | 동일 실행 권한으로 재실행 후 성공 |
| 실제 MySQL 연결 | 실행하지 않음 | Docker/MySQL 미탐지 및 Driver 미도입. 코드 실패로 분류하지 않음 |
| 설정 바인딩 전용 테스트 | 실행 대상 없음 | 저장소에 전용 테스트가 없음 |

실패를 숨기기 위해 테스트를 삭제·비활성화하거나 `@Disabled`를
추가하지 않았다.

테스트 중 Mockito/Byte Buddy의 동적 agent 로딩 관련 미래 JDK 경고가
출력됐지만 현재 테스트 실패는 아니며, 이번 단계에서 의존성을
업그레이드하지 않았다.

## 변경 파일

기존 원격 스캐폴드 재사용:

- `backend/.gitattributes`
- `backend/.gitignore`
- `backend/build.gradle`
- `backend/settings.gradle`
- `backend/gradlew`
- `backend/gradlew.bat`
- `backend/gradle/wrapper/gradle-wrapper.jar`
- `backend/gradle/wrapper/gradle-wrapper.properties`
- `backend/src/main/java/com/ssafy/billisan/BillisanApplication.java`
- `backend/src/main/resources/application.yaml`
- `backend/src/test/java/com/ssafy/billisan/BillisanApplicationTests.java`

STEP-03 최소 변경:

- `backend/.gitignore`
- `backend/.env.example`
- `backend/src/main/resources/application-local.yaml`
- `docs/backend2-setup/CONTEXT.md`
- `docs/backend2-setup/PROGRESS.md`
- `docs/backend2-setup/reports/STEP-03-SPRING-LOCAL-BASELINE.md`

## 추천 Commit 메시지

```text
S15P11A201-55 feat: Spring Boot local 개발 기준선 구성
```

## 다음 실행 프롬프트

`STEP-04`
