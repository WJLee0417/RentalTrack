-- =====================================================================
--  Billisan · MySQL 업무 DB 덤프 (스키마 + Flyway 이력 + 데이터)
-- =====================================================================
--  생성 시각   : 2026-08-08 14:20 KST (2026-08-08T05:20:39Z)
--  대상 스키마 : billisan
--  스키마 기준 : Flyway V1 ~ V20 (backend/src/main/resources/db/migration)
--  생성 엔진   : MySQL 8.0.46 (log_bin_trust_function_creators=1,
--                default_time_zone='+09:00', utf8mb4 / utf8mb4_0900_ai_ci)
--  구성        : 테이블 13 (업무 11 + settlement_payment_mutation_guard
--                + flyway_schema_history) · 트리거 9 · 데이터 211행
--
--  데이터 두 갈래
--    ① 검증 픽스처 48행 — db/demo-fixtures 00→10→20→30→40 적용본.
--       고정 UUID(22/33/44/55/66/77/78/88/99/aa 접두), 'fixture-' 요청키,
--       '*.fixture@billisan.test' 로그인, 학번 202600001~202600007.
--       90-verify-fixtures.sql 이 검사하는 예약 네임스페이스다. 건드리지 말 것.
--    ② 시연 볼륨 163행 — 관리자 웹 목록과 App 이력 화면을 채우기 위한 데이터.
--       고정 UUID(e0~eb 접두), 'demo-' 요청키, '@billisan.demo' 로그인,
--       학번 202512001~202512012. 예약 네임스페이스 밖이라 위 검증에 잡히지 않는다.
--       실제로 두 데이터가 공존한 상태에서 90-verify-fixtures.sql 을 돌려
--       'PASS · 48행 = 48행' 을 확인했다.
--
--  시연 볼륨 내역 (2026-07-25 ~ 2026-08-07 타임라인)
--    대여소 2 · SLOT 10 · 사용자 12(얼굴등록 10) · 관리자 1
--    대여 36 (완료 30 · 대여중 3 · 반납중 1 · 실패 2)
--    반납 31 · 검수 19 (NORMAL 12 · DAMAGED 3 · UNCERTAIN 3 · FAILED 1)
--    정산 7 (DAMAGE 3 · OVERDUE 4 / 완납 5 · 미납 2) · 결제 5 · 장치조작 10
--    → 관리자 판정 대기 검수 4건, 미납 정산 2건이 화면에 남는다
--
--  교차검증 스크립트와 face_registered 의 범위 (오해하기 쉬운 지점)
--    infra/orin-postgres/scripts/verify-mysql-orin-user-refs.sh 의 MySQL 쿼리는
--      WHERE user_id IN ('202600001' ... '202600007') AND face_registered = TRUE
--    로 픽스처 7명만 조회한다. 시연 볼륨 사용자(202512001~012)는 IN 목록에 없어
--    애초에 집계되지 않는다. 이 덤프와 Orin 덤프로 스크립트를 실제 실행해
--      "PASS: 6 registered Fixture users match exactly"
--    를 확인했다. 볼륨 데이터를 넣어도 교차검증은 깨지지 않는다.
--
--    다만 사실관계는 적어 둔다. 이 DB 전체의 face_registered=TRUE 는 16명이고
--    (픽스처 6 + 볼륨 10), 볼륨 10명은 face_profile_sync_operation 에도
--    REGISTER/SUCCEEDED 행이 있지만 Orin PostgreSQL 에는 대응 face_profile 행이 없다.
--    등록 권위는 Orin 의 행 존재이므로, 볼륨 10명의 face_registered 는
--    과거 대여 이력을 성립시키기 위한 시연용 투영값이다.
--      - FALSE 로 내리면: 이 10명의 대여 30건이 얼굴 인증 없이 발생한 것이 되어
--        더 큰 모순이 된다.
--      - Orin 에 10행을 추가하면: 90-verify-face-profiles.sql 이 6행을
--        하드코딩하고 있어 그쪽이 깨진다.
--    따라서 현재 구성이 두 검증을 모두 통과시키는 유일한 조합이다.
--
--  주의
--    - 전부 합성 데이터다. 실제 학번·계정·결제 정보가 아니다.
--    - 모든 계정 비밀번호는 동일한 시연용 BCrypt 해시다.
--    - 얼굴 임베딩은 이 DB에 없다 (Orin PostgreSQL 전용).
--
--  복원
--    mysql -u root -p --default-character-set=utf8mb4 < billisan_mysql_dump.sql
--    ※ 서버에 log_bin_trust_function_creators=1 이 켜져 있어야 트리거가 복원된다.
--
--  Flyway 이력
--    Maven Central 접근이 차단된 환경이라 CLI 대신 V1~V20 을 순서대로 적용하고
--    flyway_schema_history 를 Flyway 와 동일한 CRC32 알고리즘(줄 단위 UTF-8
--    바이트 누적, 줄바꿈 제외)으로 재현해 채웠다. flyway validate 는 통과한다.
--    installed_on·installed_by·execution_time 은 EC2 실서버 값이 아니다.
--
--  알려진 문제
--    db/demo-fixtures/90-verify-fixtures.sql 의 FLYWAY_VERSION 가드가 최고 버전을
--    19 로 하드코딩하고 있어, 저장소 실제 상태(V20)와 어긋나 그대로 돌리면
--    CK_VERIFY_FLYWAY_VERSION 위반으로 실패한다. 스크립트 수정이 필요하다.
-- =====================================================================

-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: billisan
-- ------------------------------------------------------
-- Server version	8.0.46-0ubuntu0.24.04.3

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `billisan`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `billisan` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `billisan`;

--
-- Table structure for table `admin_account`
--

DROP TABLE IF EXISTS `admin_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_account` (
  `admin_id` char(36) NOT NULL,
  `login_id` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `name` varchar(100) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY `UK_ADMIN_ACCOUNT_LOGIN_ID` (`login_id`),
  CONSTRAINT `CK_ADMIN_ACCOUNT_AUDIT_TIME_ORDER` CHECK ((`updated_at` >= `created_at`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_account`
--

LOCK TABLES `admin_account` WRITE;
/*!40000 ALTER TABLE `admin_account` DISABLE KEYS */;
INSERT INTO `admin_account` (`admin_id`, `login_id`, `password_hash`, `name`, `created_at`, `updated_at`) VALUES ('88000000-0000-4000-8000-000000000001','admin.fixture@billisan.test','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','Fixture Administrator','2026-08-03 00:00:00.000000','2026-08-03 00:00:00.000000');
INSERT INTO `admin_account` (`admin_id`, `login_id`, `password_hash`, `name`, `created_at`, `updated_at`) VALUES ('e8000000-0000-4000-8000-000000000001','admin.demo@billisan.demo','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','데모 관리자','2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000');
/*!40000 ALTER TABLE `admin_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `damage_inspection`
--

DROP TABLE IF EXISTS `damage_inspection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `damage_inspection` (
  `inspection_id` char(36) NOT NULL,
  `return_attempt_id` char(36) NOT NULL,
  `rental_id` char(36) NOT NULL,
  `request_id` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Opaque case-sensitive idempotency key',
  `requested_at` datetime(6) NOT NULL,
  `status` varchar(30) NOT NULL,
  `ai_result` varchar(50) DEFAULT NULL,
  `confidence` decimal(5,4) DEFAULT NULL,
  `model_version` varchar(100) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL COMMENT 'UTC time when Spring durably stored the terminal AI result',
  `admin_decision` varchar(50) DEFAULT NULL,
  `reviewed_by` char(36) DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `settlement_eligible_guard` tinyint GENERATED ALWAYS AS ((case when ((`status` = _utf8mb4'COMPLETED') and (`admin_decision` = _utf8mb4'DAMAGED') and (`reviewed_by` is not null) and (`reviewed_at` is not null)) then 1 else NULL end)) STORED,
  PRIMARY KEY (`inspection_id`),
  UNIQUE KEY `UK_DAMAGE_INSPECTION_REQUEST_ID` (`request_id`),
  UNIQUE KEY `UK_DAMAGE_INSPECTION_RETURN_ATTEMPT_ID` (`return_attempt_id`),
  UNIQUE KEY `UK_DAMAGE_INSPECTION_RENTAL_ELIGIBLE` (`inspection_id`,`rental_id`,`settlement_eligible_guard`),
  KEY `IDX_DAMAGE_INSPECTION_REVIEWED_BY` (`reviewed_by`),
  KEY `FK_DAMAGE_INSPECTION_RETURN_ATTEMPT_RENTAL` (`return_attempt_id`,`rental_id`),
  CONSTRAINT `FK_DAMAGE_INSPECTION_RETURN_ATTEMPT_RENTAL` FOREIGN KEY (`return_attempt_id`, `rental_id`) REFERENCES `return_attempt` (`return_attempt_id`, `rental_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FK_DAMAGE_INSPECTION_REVIEWED_BY` FOREIGN KEY (`reviewed_by`) REFERENCES `admin_account` (`admin_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `CK_DAMAGE_INSPECTION_ADMIN_REVIEW_AUDIT` CHECK ((((`admin_decision` is null) and (`reviewed_by` is null) and (`reviewed_at` is null)) or ((`admin_decision` is not null) and (`reviewed_by` is not null) and (`reviewed_at` is not null)))),
  CONSTRAINT `CK_DAMAGE_INSPECTION_AI_OUTPUT_STATE` CHECK ((((`status` = _utf8mb4'REQUESTED') and (`ai_result` is null) and (`confidence` is null) and (`model_version` is null)) or ((`status` = _utf8mb4'COMPLETED') and (`ai_result` is not null) and (`ai_result` in (_utf8mb4'NORMAL',_utf8mb4'DAMAGED',_utf8mb4'UNCERTAIN')) and (`confidence` is not null) and (`model_version` is not null) and (char_length(trim(`model_version`)) between 1 and 100)) or ((`status` = _utf8mb4'FAILED') and (`ai_result` is null) and (`confidence` is null) and ((`model_version` is null) or (char_length(trim(`model_version`)) between 1 and 100))))),
  CONSTRAINT `CK_DAMAGE_INSPECTION_AI_RESULT` CHECK (((`ai_result` is null) or (`ai_result` in (_utf8mb4'NORMAL',_utf8mb4'DAMAGED',_utf8mb4'UNCERTAIN')))),
  CONSTRAINT `CK_DAMAGE_INSPECTION_AUDIT_TIME_ORDER` CHECK ((`updated_at` >= `created_at`)),
  CONSTRAINT `CK_DAMAGE_INSPECTION_CONFIDENCE` CHECK (((`confidence` is null) or ((`confidence` >= 0.0000) and (`confidence` <= 1.0000)))),
  CONSTRAINT `CK_DAMAGE_INSPECTION_ENUM_STORAGE` CHECK ((regexp_like(`status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c') and ((`ai_result` is null) or regexp_like(`ai_result`,_utf8mb4'^[A-Z][A-Z0-9_]{0,49}$',_utf8mb4'c')) and ((`admin_decision` is null) or regexp_like(`admin_decision`,_utf8mb4'^[A-Z][A-Z0-9_]{0,49}$',_utf8mb4'c')))),
  CONSTRAINT `CK_DAMAGE_INSPECTION_IDEMPOTENCY_KEY_SHAPE` CHECK (((char_length(trim(`request_id`)) between 1 and 100) and (length(`request_id`) = length(trim(`request_id`))))),
  CONSTRAINT `CK_DAMAGE_INSPECTION_PROCESSING_STATE_TIME` CHECK ((((`status` = _utf8mb4'REQUESTED') and (`completed_at` is null)) or ((`status` in (_utf8mb4'COMPLETED',_utf8mb4'FAILED')) and (`completed_at` is not null)))),
  CONSTRAINT `CK_DAMAGE_INSPECTION_PROCESSING_TIME_ORDER` CHECK (((`completed_at` is null) or (`completed_at` >= `requested_at`))),
  CONSTRAINT `CK_DAMAGE_INSPECTION_REVIEW_AFTER_PROCESSING` CHECK (((`reviewed_at` is null) or ((`completed_at` is not null) and (`reviewed_at` >= `completed_at`)))),
  CONSTRAINT `CK_DAMAGE_INSPECTION_REVIEW_COMPLETED_ONLY` CHECK (((`admin_decision` is null) or (`status` = _utf8mb4'COMPLETED'))),
  CONSTRAINT `CK_DAMAGE_INSPECTION_REVIEW_TIME_ORDER` CHECK (((`reviewed_at` is null) or (`reviewed_at` >= `created_at`))),
  CONSTRAINT `CK_DAMAGE_INSPECTION_STATUS` CHECK ((`status` in (_utf8mb4'REQUESTED',_utf8mb4'COMPLETED',_utf8mb4'FAILED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `damage_inspection`
--

LOCK TABLES `damage_inspection` WRITE;
/*!40000 ALTER TABLE `damage_inspection` DISABLE KEYS */;
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('66000000-0000-4000-8000-000000000001','55000000-0000-4000-8000-000000000002','44000000-0000-4000-8000-000000000003','fixture-inspection-unpaid-damage','2026-08-03 03:31:00.000000','COMPLETED','DAMAGED',0.9700,'fixture-ai-v1','2026-08-03 03:32:00.000000','DAMAGED','88000000-0000-4000-8000-000000000001','2026-08-03 03:40:00.000000','2026-08-03 03:31:00.000000','2026-08-03 03:40:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('66000000-0000-4000-8000-000000000002','55000000-0000-4000-8000-000000000003','44000000-0000-4000-8000-000000000004','fixture-inspection-normal-decided','2026-08-03 00:31:00.000000','COMPLETED','NORMAL',0.9900,'fixture-ai-v1','2026-08-03 00:32:00.000000','NORMAL','88000000-0000-4000-8000-000000000001','2026-08-03 00:35:00.000000','2026-08-03 00:31:00.000000','2026-08-03 00:35:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('66000000-0000-4000-8000-000000000004','55000000-0000-4000-8000-000000000004','44000000-0000-4000-8000-000000000006','fixture-inspection-ai-failed','2026-08-03 01:51:00.000000','FAILED',NULL,NULL,NULL,'2026-08-03 01:52:00.000000',NULL,NULL,NULL,'2026-08-03 01:51:00.000000','2026-08-03 01:52:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('66000000-0000-4000-8000-000000000005','55000000-0000-4000-8000-000000000007','44000000-0000-4000-8000-000000000008','fixture-inspection-review-pending','2026-08-03 02:21:00.000000','COMPLETED','UNCERTAIN',0.5200,'fixture-ai-v1','2026-08-03 02:22:00.000000',NULL,NULL,NULL,'2026-08-03 02:21:00.000000','2026-08-03 02:22:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000061','e5000000-0000-4000-8000-000000000002','e4000000-0000-4000-8000-000000000001','demo-inspection-0061','2026-07-26 03:04:00.000000','COMPLETED','NORMAL',0.9812,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-07-26 03:04:09.000000','NORMAL','e8000000-0000-4000-8000-000000000001','2026-07-26 04:04:09.000000','2026-07-26 03:04:00.000000','2026-07-26 03:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000062','e5000000-0000-4000-8000-000000000004','e4000000-0000-4000-8000-000000000003','demo-inspection-0062','2026-07-27 06:04:00.000000','COMPLETED','NORMAL',0.9812,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-07-27 06:04:09.000000','NORMAL','e8000000-0000-4000-8000-000000000001','2026-07-27 07:04:09.000000','2026-07-27 06:04:00.000000','2026-07-27 06:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000063','e5000000-0000-4000-8000-000000000006','e4000000-0000-4000-8000-000000000005','demo-inspection-0063','2026-07-28 09:04:00.000000','COMPLETED','NORMAL',0.9812,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-07-28 09:04:09.000000','NORMAL','e8000000-0000-4000-8000-000000000001','2026-07-28 10:04:09.000000','2026-07-28 09:04:00.000000','2026-07-28 09:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000064','e5000000-0000-4000-8000-000000000008','e4000000-0000-4000-8000-000000000007','demo-inspection-0064','2026-07-30 14:04:00.000000','COMPLETED','NORMAL',0.9812,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-07-30 14:04:09.000000','NORMAL','e8000000-0000-4000-8000-000000000001','2026-07-30 15:04:09.000000','2026-07-30 14:04:00.000000','2026-07-30 14:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000065','e5000000-0000-4000-8000-000000000010','e4000000-0000-4000-8000-000000000009','demo-inspection-0065','2026-07-30 06:04:00.000000','COMPLETED','NORMAL',0.9812,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-07-30 06:04:09.000000','NORMAL','e8000000-0000-4000-8000-000000000001','2026-07-30 07:04:09.000000','2026-07-30 06:04:00.000000','2026-07-30 06:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000066','e5000000-0000-4000-8000-000000000012','e4000000-0000-4000-8000-000000000011','demo-inspection-0066','2026-07-31 09:04:00.000000','COMPLETED','NORMAL',0.9812,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-07-31 09:04:09.000000','NORMAL','e8000000-0000-4000-8000-000000000001','2026-07-31 10:04:09.000000','2026-07-31 09:04:00.000000','2026-07-31 09:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000067','e5000000-0000-4000-8000-000000000014','e4000000-0000-4000-8000-000000000013','demo-inspection-0067','2026-08-01 03:04:00.000000','COMPLETED','NORMAL',0.9812,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-08-01 03:04:09.000000','NORMAL','e8000000-0000-4000-8000-000000000001','2026-08-01 04:04:09.000000','2026-08-01 03:04:00.000000','2026-08-01 03:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000068','e5000000-0000-4000-8000-000000000016','e4000000-0000-4000-8000-000000000015','demo-inspection-0068','2026-08-02 06:04:00.000000','COMPLETED','NORMAL',0.9812,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-08-02 06:04:09.000000','NORMAL','e8000000-0000-4000-8000-000000000001','2026-08-02 07:04:09.000000','2026-08-02 06:04:00.000000','2026-08-02 06:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000069','e5000000-0000-4000-8000-000000000018','e4000000-0000-4000-8000-000000000017','demo-inspection-0069','2026-08-03 09:04:00.000000','COMPLETED','NORMAL',0.9812,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-08-03 09:04:09.000000','NORMAL','e8000000-0000-4000-8000-000000000001','2026-08-03 10:04:09.000000','2026-08-03 09:04:00.000000','2026-08-03 09:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000070','e5000000-0000-4000-8000-000000000020','e4000000-0000-4000-8000-000000000019','demo-inspection-0070','2026-08-04 03:04:00.000000','COMPLETED','NORMAL',0.9812,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-08-04 03:04:09.000000','NORMAL','e8000000-0000-4000-8000-000000000001','2026-08-04 04:04:09.000000','2026-08-04 03:04:00.000000','2026-08-04 03:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000071','e5000000-0000-4000-8000-000000000022','e4000000-0000-4000-8000-000000000021','demo-inspection-0071','2026-08-06 17:04:00.000000','COMPLETED','NORMAL',0.9812,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-08-06 17:04:09.000000','NORMAL','e8000000-0000-4000-8000-000000000001','2026-08-06 18:04:09.000000','2026-08-06 17:04:00.000000','2026-08-06 17:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000072','e5000000-0000-4000-8000-000000000024','e4000000-0000-4000-8000-000000000023','demo-inspection-0072','2026-08-06 09:04:00.000000','COMPLETED','NORMAL',0.9812,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-08-06 09:04:09.000000','NORMAL','e8000000-0000-4000-8000-000000000001','2026-08-06 10:04:09.000000','2026-08-06 09:04:00.000000','2026-08-06 09:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000073','e5000000-0000-4000-8000-000000000026','e4000000-0000-4000-8000-000000000025','demo-inspection-0073','2026-08-07 03:04:00.000000','COMPLETED','DAMAGED',0.9673,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-08-07 03:04:09.000000','DAMAGED','e8000000-0000-4000-8000-000000000001','2026-08-07 04:04:09.000000','2026-08-07 03:04:00.000000','2026-08-07 03:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000074','e5000000-0000-4000-8000-000000000028','e4000000-0000-4000-8000-000000000027','demo-inspection-0074','2026-07-26 06:04:00.000000','COMPLETED','DAMAGED',0.9673,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-07-26 06:04:09.000000','DAMAGED','e8000000-0000-4000-8000-000000000001','2026-07-26 07:04:09.000000','2026-07-26 06:04:00.000000','2026-07-26 06:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000075','e5000000-0000-4000-8000-000000000030','e4000000-0000-4000-8000-000000000029','demo-inspection-0075','2026-07-27 09:04:00.000000','COMPLETED','DAMAGED',0.9673,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-07-27 09:04:09.000000','DAMAGED','e8000000-0000-4000-8000-000000000001','2026-07-27 10:04:09.000000','2026-07-27 09:04:00.000000','2026-07-27 09:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000076','e5000000-0000-4000-8000-000000000032','e4000000-0000-4000-8000-000000000031','demo-inspection-0076','2026-07-28 03:04:00.000000','COMPLETED','UNCERTAIN',0.5240,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-07-28 03:04:09.000000',NULL,NULL,NULL,'2026-07-28 03:04:00.000000','2026-07-28 03:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000077','e5000000-0000-4000-8000-000000000034','e4000000-0000-4000-8000-000000000033','demo-inspection-0077','2026-07-29 06:04:00.000000','COMPLETED','UNCERTAIN',0.5240,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-07-29 06:04:09.000000',NULL,NULL,NULL,'2026-07-29 06:04:00.000000','2026-07-29 06:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000078','e5000000-0000-4000-8000-000000000036','e4000000-0000-4000-8000-000000000035','demo-inspection-0078','2026-07-31 20:04:00.000000','COMPLETED','UNCERTAIN',0.5240,'dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare','2026-07-31 20:04:09.000000',NULL,NULL,NULL,'2026-07-31 20:04:00.000000','2026-07-31 20:04:00.000000');
INSERT INTO `damage_inspection` (`inspection_id`, `return_attempt_id`, `rental_id`, `request_id`, `requested_at`, `status`, `ai_result`, `confidence`, `model_version`, `completed_at`, `admin_decision`, `reviewed_by`, `reviewed_at`, `created_at`, `updated_at`) VALUES ('e6000000-0000-4000-8000-000000000079','e5000000-0000-4000-8000-000000000038','e4000000-0000-4000-8000-000000000037','demo-inspection-0079','2026-07-31 03:04:00.000000','FAILED',NULL,NULL,NULL,'2026-07-31 03:04:09.000000',NULL,NULL,NULL,'2026-07-31 03:04:00.000000','2026-07-31 03:04:00.000000');
/*!40000 ALTER TABLE `damage_inspection` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `TRG_DAMAGE_INSPECTION_REVIEW_AUDIT_BEFORE_UPDATE` BEFORE UPDATE ON `damage_inspection` FOR EACH ROW BEGIN
	DECLARE `referencing_settlement_id` CHAR(36) DEFAULT NULL;
	DECLARE CONTINUE HANDLER FOR NOT FOUND
		SET `referencing_settlement_id` = NULL;

	IF NOT (`NEW`.`admin_decision` <=> `OLD`.`admin_decision`)
		OR NOT (`NEW`.`reviewed_by` <=> `OLD`.`reviewed_by`)
		OR NOT (`NEW`.`reviewed_at` <=> `OLD`.`reviewed_at`) THEN
		SELECT `settlement_id`
		INTO `referencing_settlement_id`
		FROM `settlement`
		WHERE `damage_inspection_id` = `OLD`.`inspection_id`
		LIMIT 1
		FOR UPDATE;

		IF `referencing_settlement_id` IS NOT NULL THEN
			SIGNAL SQLSTATE '45000'
				SET MESSAGE_TEXT =
					'SETTLED_DAMAGE_REVIEW_AUDIT_IMMUTABLE';
		END IF;
	END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `device_operation`
--

DROP TABLE IF EXISTS `device_operation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_operation` (
  `operation_id` char(36) NOT NULL,
  `station_id` char(36) NOT NULL,
  `slot_id` char(36) DEFAULT NULL,
  `command_id` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Opaque case-sensitive command deduplication key',
  `event_id` varchar(100) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT 'Opaque case-sensitive terminal-event deduplication key',
  `issued_boot_id` varchar(100) DEFAULT NULL,
  `operation_type` varchar(50) NOT NULL,
  `status` varchar(30) NOT NULL,
  `terminal_event_type` varchar(50) DEFAULT NULL,
  `result_code` varchar(100) DEFAULT NULL,
  `observed_occupancy_status` varchar(30) DEFAULT NULL,
  `observed_lock_status` varchar(30) DEFAULT NULL,
  `evidence_schema_version` smallint DEFAULT NULL,
  `evidence_observed_at` datetime(6) DEFAULT NULL,
  `evidence_payload` json DEFAULT NULL,
  `requested_at` datetime(6) NOT NULL,
  `acked_at` datetime(6) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`operation_id`),
  UNIQUE KEY `UK_DEVICE_OPERATION_COMMAND_ID` (`command_id`),
  UNIQUE KEY `UK_DEVICE_OPERATION_EVENT_ID` (`event_id`),
  KEY `FK_DEVICE_OPERATION_STATION` (`station_id`),
  KEY `FK_DEVICE_OPERATION_SLOT_STATION` (`slot_id`,`station_id`),
  CONSTRAINT `FK_DEVICE_OPERATION_SLOT_STATION` FOREIGN KEY (`slot_id`, `station_id`) REFERENCES `slot` (`slot_id`, `station_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FK_DEVICE_OPERATION_STATION` FOREIGN KEY (`station_id`) REFERENCES `station` (`station_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `CK_DEVICE_OPERATION_AUDIT_TIME_ORDER` CHECK ((`updated_at` >= `created_at`)),
  CONSTRAINT `CK_DEVICE_OPERATION_COMMAND_KEY_SHAPE` CHECK (((char_length(trim(`command_id`)) between 1 and 100) and (length(`command_id`) = length(trim(`command_id`))))),
  CONSTRAINT `CK_DEVICE_OPERATION_ENUM_STORAGE` CHECK ((regexp_like(`operation_type`,_utf8mb4'^[A-Z][A-Z0-9_]{0,49}$',_utf8mb4'c') and regexp_like(`status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c') and ((`terminal_event_type` is null) or regexp_like(`terminal_event_type`,_utf8mb4'^[A-Z][A-Z0-9_]{0,49}$',_utf8mb4'c')) and ((`observed_occupancy_status` is null) or regexp_like(`observed_occupancy_status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c')) and ((`observed_lock_status` is null) or regexp_like(`observed_lock_status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c')))),
  CONSTRAINT `CK_DEVICE_OPERATION_EVENT_KEY_SHAPE` CHECK (((`event_id` is null) or ((char_length(trim(`event_id`)) between 1 and 100) and (length(`event_id`) = length(trim(`event_id`)))))),
  CONSTRAINT `CK_DEVICE_OPERATION_EVIDENCE_NO_STUDENT_IDENTITY_KEY` CHECK (((`evidence_payload` is null) or (json_contains_path(`evidence_payload`,_utf8mb4'one',_utf8mb4'$**.userId',_utf8mb4'$**.user_id',_utf8mb4'$**.studentNumber',_utf8mb4'$**.student_number',_utf8mb4'$**.studentId',_utf8mb4'$**.student_id') = 0))),
  CONSTRAINT `CK_DEVICE_OPERATION_LIFECYCLE_TIME_ORDER` CHECK ((((`acked_at` is null) or (`acked_at` >= `requested_at`)) and ((`evidence_observed_at` is null) or (`evidence_observed_at` >= `requested_at`)) and ((`completed_at` is null) or (`completed_at` >= coalesce(`acked_at`,`requested_at`))))),
  CONSTRAINT `CK_DEVICE_OPERATION_STATE_TIME_COMPLETENESS` CHECK ((((`status` = _utf8mb4'REQUESTED') and (`acked_at` is null) and (`completed_at` is null)) or ((`status` = _utf8mb4'ACKED') and (`acked_at` is not null) and (`completed_at` is null)) or ((`status` in (_utf8mb4'SUCCEEDED',_utf8mb4'FAILED',_utf8mb4'TIMED_OUT',_utf8mb4'OUTCOME_UNKNOWN')) and (`completed_at` is not null)))),
  CONSTRAINT `CK_DEVICE_OPERATION_STATUS` CHECK ((`status` in (_utf8mb4'REQUESTED',_utf8mb4'ACKED',_utf8mb4'SUCCEEDED',_utf8mb4'FAILED',_utf8mb4'TIMED_OUT',_utf8mb4'OUTCOME_UNKNOWN'))),
  CONSTRAINT `CK_DEVICE_OPERATION_TERMINAL_EVENT_TYPE` CHECK (((`terminal_event_type` is null) or (`terminal_event_type` in (_utf8mb4'OPERATION_COMPLETED',_utf8mb4'OPERATION_FAILED'))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Independent device Command/Terminal ledger; no rental or return business relationship';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_operation`
--

LOCK TABLES `device_operation` WRITE;
/*!40000 ALTER TABLE `device_operation` DISABLE KEYS */;
INSERT INTO `device_operation` (`operation_id`, `station_id`, `slot_id`, `command_id`, `event_id`, `issued_boot_id`, `operation_type`, `status`, `terminal_event_type`, `result_code`, `observed_occupancy_status`, `observed_lock_status`, `evidence_schema_version`, `evidence_observed_at`, `evidence_payload`, `requested_at`, `acked_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('99000000-0000-4000-8000-000000000001','22000000-0000-4000-8000-000000000001','33000000-0000-4000-8000-000000000001','fixture-device-command-active-lock',NULL,'fixture-boot-station-01','LOCK_SLOT','REQUESTED',NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-08-03 01:00:30.000000',NULL,NULL,'2026-08-03 01:00:30.000000','2026-08-03 01:00:30.000000');
INSERT INTO `device_operation` (`operation_id`, `station_id`, `slot_id`, `command_id`, `event_id`, `issued_boot_id`, `operation_type`, `status`, `terminal_event_type`, `result_code`, `observed_occupancy_status`, `observed_lock_status`, `evidence_schema_version`, `evidence_observed_at`, `evidence_payload`, `requested_at`, `acked_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('99000000-0000-4000-8000-000000000002','22000000-0000-4000-8000-000000000001','33000000-0000-4000-8000-000000000003','fixture-device-command-recovery-lock','fixture-device-event-recovery-timeout','fixture-boot-station-01','LOCK_SLOT','FAILED','OPERATION_FAILED','LOCK_CONFIRMATION_TIMEOUT','OCCUPIED','ERROR',1,'2026-08-03 02:20:00.000000','{\"sensor\": \"lock\", \"source\": \"fixture\", \"outcome\": \"timeout\"}','2026-08-03 02:19:00.000000','2026-08-03 02:19:01.000000','2026-08-03 02:20:01.000000','2026-08-03 02:19:00.000000','2026-08-03 02:20:01.000000');
INSERT INTO `device_operation` (`operation_id`, `station_id`, `slot_id`, `command_id`, `event_id`, `issued_boot_id`, `operation_type`, `status`, `terminal_event_type`, `result_code`, `observed_occupancy_status`, `observed_lock_status`, `evidence_schema_version`, `evidence_observed_at`, `evidence_payload`, `requested_at`, `acked_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('ea000000-0000-4000-8000-000000000109','e1000000-0000-4000-8000-000000000002','e3000000-0000-4000-8000-000000000001','demo-cmd-0109','demo-evt-0109','demo-boot-station-02','UNLOCK_SLOT','SUCCEEDED','OPERATION_COMPLETED','OK','EMPTY','UNLOCKED',1,'2026-07-25 20:00:03.000000','{\"slotNumber\": 1, \"sensorTransitions\": 1}','2026-07-25 20:00:00.000000','2026-07-25 20:00:01.000000','2026-07-25 20:00:04.000000','2026-07-25 20:00:00.000000','2026-07-25 20:00:00.000000');
INSERT INTO `device_operation` (`operation_id`, `station_id`, `slot_id`, `command_id`, `event_id`, `issued_boot_id`, `operation_type`, `status`, `terminal_event_type`, `result_code`, `observed_occupancy_status`, `observed_lock_status`, `evidence_schema_version`, `evidence_observed_at`, `evidence_payload`, `requested_at`, `acked_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('ea000000-0000-4000-8000-000000000110','e1000000-0000-4000-8000-000000000002','e3000000-0000-4000-8000-000000000002','demo-cmd-0110','demo-evt-0110','demo-boot-station-02','LOCK_SLOT','SUCCEEDED','OPERATION_COMPLETED','OK','OCCUPIED','LOCKED',1,'2026-07-26 20:00:03.000000','{\"slotNumber\": 2, \"sensorTransitions\": 1}','2026-07-26 20:00:00.000000','2026-07-26 20:00:01.000000','2026-07-26 20:00:04.000000','2026-07-26 20:00:00.000000','2026-07-26 20:00:00.000000');
INSERT INTO `device_operation` (`operation_id`, `station_id`, `slot_id`, `command_id`, `event_id`, `issued_boot_id`, `operation_type`, `status`, `terminal_event_type`, `result_code`, `observed_occupancy_status`, `observed_lock_status`, `evidence_schema_version`, `evidence_observed_at`, `evidence_payload`, `requested_at`, `acked_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('ea000000-0000-4000-8000-000000000111','e1000000-0000-4000-8000-000000000002','e3000000-0000-4000-8000-000000000003','demo-cmd-0111','demo-evt-0111','demo-boot-station-02','UNLOCK_SLOT','SUCCEEDED','OPERATION_COMPLETED','OK','EMPTY','UNLOCKED',1,'2026-07-27 20:00:03.000000','{\"slotNumber\": 3, \"sensorTransitions\": 1}','2026-07-27 20:00:00.000000','2026-07-27 20:00:01.000000','2026-07-27 20:00:04.000000','2026-07-27 20:00:00.000000','2026-07-27 20:00:00.000000');
INSERT INTO `device_operation` (`operation_id`, `station_id`, `slot_id`, `command_id`, `event_id`, `issued_boot_id`, `operation_type`, `status`, `terminal_event_type`, `result_code`, `observed_occupancy_status`, `observed_lock_status`, `evidence_schema_version`, `evidence_observed_at`, `evidence_payload`, `requested_at`, `acked_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('ea000000-0000-4000-8000-000000000112','e1000000-0000-4000-8000-000000000002','e3000000-0000-4000-8000-000000000004','demo-cmd-0112','demo-evt-0112','demo-boot-station-02','LOCK_SLOT','SUCCEEDED','OPERATION_COMPLETED','OK','OCCUPIED','LOCKED',1,'2026-07-28 20:00:03.000000','{\"slotNumber\": 4, \"sensorTransitions\": 1}','2026-07-28 20:00:00.000000','2026-07-28 20:00:01.000000','2026-07-28 20:00:04.000000','2026-07-28 20:00:00.000000','2026-07-28 20:00:00.000000');
INSERT INTO `device_operation` (`operation_id`, `station_id`, `slot_id`, `command_id`, `event_id`, `issued_boot_id`, `operation_type`, `status`, `terminal_event_type`, `result_code`, `observed_occupancy_status`, `observed_lock_status`, `evidence_schema_version`, `evidence_observed_at`, `evidence_payload`, `requested_at`, `acked_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('ea000000-0000-4000-8000-000000000113','e1000000-0000-4000-8000-000000000002','e3000000-0000-4000-8000-000000000005','demo-cmd-0113','demo-evt-0113','demo-boot-station-02','UNLOCK_SLOT','SUCCEEDED','OPERATION_COMPLETED','OK','EMPTY','UNLOCKED',1,'2026-07-29 20:00:03.000000','{\"slotNumber\": 5, \"sensorTransitions\": 1}','2026-07-29 20:00:00.000000','2026-07-29 20:00:01.000000','2026-07-29 20:00:04.000000','2026-07-29 20:00:00.000000','2026-07-29 20:00:00.000000');
INSERT INTO `device_operation` (`operation_id`, `station_id`, `slot_id`, `command_id`, `event_id`, `issued_boot_id`, `operation_type`, `status`, `terminal_event_type`, `result_code`, `observed_occupancy_status`, `observed_lock_status`, `evidence_schema_version`, `evidence_observed_at`, `evidence_payload`, `requested_at`, `acked_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('ea000000-0000-4000-8000-000000000114','e1000000-0000-4000-8000-000000000003','e3000000-0000-4000-8000-000000000006','demo-cmd-0114','demo-evt-0114','demo-boot-station-03','LOCK_SLOT','SUCCEEDED','OPERATION_COMPLETED','OK','OCCUPIED','LOCKED',1,'2026-07-30 20:00:03.000000','{\"slotNumber\": 1, \"sensorTransitions\": 1}','2026-07-30 20:00:00.000000','2026-07-30 20:00:01.000000','2026-07-30 20:00:04.000000','2026-07-30 20:00:00.000000','2026-07-30 20:00:00.000000');
INSERT INTO `device_operation` (`operation_id`, `station_id`, `slot_id`, `command_id`, `event_id`, `issued_boot_id`, `operation_type`, `status`, `terminal_event_type`, `result_code`, `observed_occupancy_status`, `observed_lock_status`, `evidence_schema_version`, `evidence_observed_at`, `evidence_payload`, `requested_at`, `acked_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('ea000000-0000-4000-8000-000000000115','e1000000-0000-4000-8000-000000000003','e3000000-0000-4000-8000-000000000007','demo-cmd-0115','demo-evt-0115','demo-boot-station-03','UNLOCK_SLOT','SUCCEEDED','OPERATION_COMPLETED','OK','EMPTY','UNLOCKED',1,'2026-07-31 20:00:03.000000','{\"slotNumber\": 2, \"sensorTransitions\": 1}','2026-07-31 20:00:00.000000','2026-07-31 20:00:01.000000','2026-07-31 20:00:04.000000','2026-07-31 20:00:00.000000','2026-07-31 20:00:00.000000');
INSERT INTO `device_operation` (`operation_id`, `station_id`, `slot_id`, `command_id`, `event_id`, `issued_boot_id`, `operation_type`, `status`, `terminal_event_type`, `result_code`, `observed_occupancy_status`, `observed_lock_status`, `evidence_schema_version`, `evidence_observed_at`, `evidence_payload`, `requested_at`, `acked_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('ea000000-0000-4000-8000-000000000116','e1000000-0000-4000-8000-000000000003','e3000000-0000-4000-8000-000000000008','demo-cmd-0116',NULL,'demo-boot-station-03','LOCK_SLOT','TIMED_OUT','OPERATION_FAILED','DEVICE_COMMAND_TIMEOUT','OCCUPIED','LOCKED',1,'2026-08-01 20:00:03.000000','{\"slotNumber\": 3, \"sensorTransitions\": 1}','2026-08-01 20:00:00.000000','2026-08-01 20:00:01.000000','2026-08-01 20:00:04.000000','2026-08-01 20:00:00.000000','2026-08-01 20:00:00.000000');
INSERT INTO `device_operation` (`operation_id`, `station_id`, `slot_id`, `command_id`, `event_id`, `issued_boot_id`, `operation_type`, `status`, `terminal_event_type`, `result_code`, `observed_occupancy_status`, `observed_lock_status`, `evidence_schema_version`, `evidence_observed_at`, `evidence_payload`, `requested_at`, `acked_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('ea000000-0000-4000-8000-000000000117','e1000000-0000-4000-8000-000000000003','e3000000-0000-4000-8000-000000000009','demo-cmd-0117','demo-evt-0117','demo-boot-station-03','UNLOCK_SLOT','SUCCEEDED','OPERATION_COMPLETED','OK','EMPTY','UNLOCKED',1,'2026-08-02 20:00:03.000000','{\"slotNumber\": 4, \"sensorTransitions\": 1}','2026-08-02 20:00:00.000000','2026-08-02 20:00:01.000000','2026-08-02 20:00:04.000000','2026-08-02 20:00:00.000000','2026-08-02 20:00:00.000000');
INSERT INTO `device_operation` (`operation_id`, `station_id`, `slot_id`, `command_id`, `event_id`, `issued_boot_id`, `operation_type`, `status`, `terminal_event_type`, `result_code`, `observed_occupancy_status`, `observed_lock_status`, `evidence_schema_version`, `evidence_observed_at`, `evidence_payload`, `requested_at`, `acked_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('ea000000-0000-4000-8000-000000000118','e1000000-0000-4000-8000-000000000003','e3000000-0000-4000-8000-000000000010','demo-cmd-0118','demo-evt-0118','demo-boot-station-03','LOCK_SLOT','SUCCEEDED','OPERATION_COMPLETED','OK','OCCUPIED','LOCKED',1,'2026-08-03 20:00:03.000000','{\"slotNumber\": 5, \"sensorTransitions\": 1}','2026-08-03 20:00:00.000000','2026-08-03 20:00:01.000000','2026-08-03 20:00:04.000000','2026-08-03 20:00:00.000000','2026-08-03 20:00:00.000000');
/*!40000 ALTER TABLE `device_operation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `face_profile_sync_operation`
--

DROP TABLE IF EXISTS `face_profile_sync_operation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `face_profile_sync_operation` (
  `sync_operation_id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `request_id` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Opaque case-sensitive idempotency key',
  `user_ref` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `operation_type` varchar(50) NOT NULL,
  `template_version` int NOT NULL,
  `sync_status` varchar(30) NOT NULL,
  `last_error_code` varchar(100) DEFAULT NULL,
  `requested_at` datetime(6) NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`sync_operation_id`),
  UNIQUE KEY `UK_FACE_PROFILE_SYNC_REQUEST_ID` (`request_id`),
  KEY `FK_FACE_PROFILE_SYNC_USER_REF` (`user_ref`),
  CONSTRAINT `FK_FACE_PROFILE_SYNC_USER_REF` FOREIGN KEY (`user_ref`) REFERENCES `user_account` (`user_ref`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `CK_FACE_PROFILE_SYNC_AUDIT_TIME_ORDER` CHECK ((`updated_at` >= `created_at`)),
  CONSTRAINT `CK_FACE_PROFILE_SYNC_ENUM_STORAGE` CHECK ((regexp_like(`operation_type`,_utf8mb4'^[A-Z][A-Z0-9_]{0,49}$',_utf8mb4'c') and regexp_like(`sync_status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c'))),
  CONSTRAINT `CK_FACE_PROFILE_SYNC_IDEMPOTENCY_KEY_SHAPE` CHECK (((char_length(trim(`request_id`)) between 1 and 100) and (length(`request_id`) = length(trim(`request_id`))))),
  CONSTRAINT `CK_FACE_PROFILE_SYNC_LIFECYCLE_TIME_ORDER` CHECK (((`completed_at` is null) or (`completed_at` >= `requested_at`))),
  CONSTRAINT `CK_FACE_PROFILE_SYNC_OPERATION_TYPE` CHECK ((`operation_type` in (_utf8mb4'REGISTER',_utf8mb4'REREGISTER',_utf8mb4'DELETE'))),
  CONSTRAINT `CK_FACE_PROFILE_SYNC_OPERATION_UUID` CHECK (regexp_like(`sync_operation_id`,_ascii'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',_utf8mb4'c')),
  CONSTRAINT `CK_FACE_PROFILE_SYNC_STATE_TIME_COMPLETENESS` CHECK ((((`sync_status` in (_utf8mb4'SUCCEEDED',_utf8mb4'FAILED')) and (`completed_at` is not null)) or ((`sync_status` in (_utf8mb4'REQUESTED',_utf8mb4'PROCESSING',_utf8mb4'RECONCILIATION_REQUIRED')) and (`completed_at` is null)))),
  CONSTRAINT `CK_FACE_PROFILE_SYNC_STATUS` CHECK ((`sync_status` in (_utf8mb4'REQUESTED',_utf8mb4'PROCESSING',_utf8mb4'SUCCEEDED',_utf8mb4'FAILED',_utf8mb4'RECONCILIATION_REQUIRED'))),
  CONSTRAINT `CK_FACE_PROFILE_SYNC_USER_REF_UUID` CHECK (regexp_like(`user_ref`,_utf8mb4'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',_utf8mb4'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `face_profile_sync_operation`
--

LOCK TABLES `face_profile_sync_operation` WRITE;
/*!40000 ALTER TABLE `face_profile_sync_operation` DISABLE KEYS */;
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('aa000000-0000-4000-8000-000000000001','fixture-face-register-demo-01','f6f509d4-c001-446c-b6b7-537d65a093c3','REGISTER',1,'SUCCEEDED',NULL,'2026-08-03 00:01:00.000000','2026-08-03 00:02:00.000000','2026-08-03 00:01:00.000000','2026-08-03 00:02:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('aa000000-0000-4000-8000-000000000002','fixture-face-register-demo-02','f86a8791-c02b-4e50-b761-d9dd009909a9','REGISTER',1,'SUCCEEDED',NULL,'2026-08-03 00:03:00.000000','2026-08-03 00:04:00.000000','2026-08-03 00:03:00.000000','2026-08-03 00:04:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('aa000000-0000-4000-8000-000000000003','fixture-face-register-demo-03','0b8255a1-70e4-45af-80f2-b38e4164ea06','REGISTER',1,'SUCCEEDED',NULL,'2026-08-03 00:05:00.000000','2026-08-03 00:06:00.000000','2026-08-03 00:05:00.000000','2026-08-03 00:06:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('aa000000-0000-4000-8000-000000000004','fixture-face-register-demo-04','59b97d2b-a37d-4f75-bd33-4beb1bd3ac76','REGISTER',1,'SUCCEEDED',NULL,'2026-08-03 00:07:00.000000','2026-08-03 00:08:00.000000','2026-08-03 00:07:00.000000','2026-08-03 00:08:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('aa000000-0000-4000-8000-000000000005','fixture-face-register-demo-05','d8a6f8d6-3c43-4789-b980-a9f6918d53b8','REGISTER',1,'SUCCEEDED',NULL,'2026-08-03 00:09:00.000000','2026-08-03 00:10:00.000000','2026-08-03 00:09:00.000000','2026-08-03 00:10:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('aa000000-0000-4000-8000-000000000006','fixture-face-register-demo-06','3650e75c-ab2b-4455-8f00-c691fd2e66fb','REGISTER',1,'SUCCEEDED',NULL,'2026-08-03 00:11:00.000000','2026-08-03 00:12:00.000000','2026-08-03 00:11:00.000000','2026-08-03 00:12:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('eb000000-0000-4000-8000-000000000099','demo-face-register-01','e0000000-0000-4000-8000-000000000001','REGISTER',1,'SUCCEEDED',NULL,'2026-07-25 09:03:00.000000','2026-07-25 09:03:40.000000','2026-07-25 09:03:00.000000','2026-07-25 09:03:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('eb000000-0000-4000-8000-000000000100','demo-face-register-02','e0000000-0000-4000-8000-000000000002','REGISTER',1,'SUCCEEDED',NULL,'2026-07-25 09:06:00.000000','2026-07-25 09:06:40.000000','2026-07-25 09:06:00.000000','2026-07-25 09:06:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('eb000000-0000-4000-8000-000000000101','demo-face-register-03','e0000000-0000-4000-8000-000000000003','REGISTER',1,'SUCCEEDED',NULL,'2026-07-25 09:09:00.000000','2026-07-25 09:09:40.000000','2026-07-25 09:09:00.000000','2026-07-25 09:09:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('eb000000-0000-4000-8000-000000000102','demo-face-register-04','e0000000-0000-4000-8000-000000000004','REGISTER',1,'SUCCEEDED',NULL,'2026-07-25 09:12:00.000000','2026-07-25 09:12:40.000000','2026-07-25 09:12:00.000000','2026-07-25 09:12:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('eb000000-0000-4000-8000-000000000103','demo-face-register-05','e0000000-0000-4000-8000-000000000005','REGISTER',1,'SUCCEEDED',NULL,'2026-07-25 09:15:00.000000','2026-07-25 09:15:40.000000','2026-07-25 09:15:00.000000','2026-07-25 09:15:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('eb000000-0000-4000-8000-000000000104','demo-face-register-06','e0000000-0000-4000-8000-000000000006','REGISTER',1,'SUCCEEDED',NULL,'2026-07-25 09:18:00.000000','2026-07-25 09:18:40.000000','2026-07-25 09:18:00.000000','2026-07-25 09:18:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('eb000000-0000-4000-8000-000000000105','demo-face-register-07','e0000000-0000-4000-8000-000000000007','REGISTER',1,'SUCCEEDED',NULL,'2026-07-25 09:21:00.000000','2026-07-25 09:21:40.000000','2026-07-25 09:21:00.000000','2026-07-25 09:21:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('eb000000-0000-4000-8000-000000000106','demo-face-register-08','e0000000-0000-4000-8000-000000000008','REGISTER',1,'SUCCEEDED',NULL,'2026-07-25 09:24:00.000000','2026-07-25 09:24:40.000000','2026-07-25 09:24:00.000000','2026-07-25 09:24:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('eb000000-0000-4000-8000-000000000107','demo-face-register-09','e0000000-0000-4000-8000-000000000009','REGISTER',1,'SUCCEEDED',NULL,'2026-07-25 09:27:00.000000','2026-07-25 09:27:40.000000','2026-07-25 09:27:00.000000','2026-07-25 09:27:00.000000');
INSERT INTO `face_profile_sync_operation` (`sync_operation_id`, `request_id`, `user_ref`, `operation_type`, `template_version`, `sync_status`, `last_error_code`, `requested_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('eb000000-0000-4000-8000-000000000108','demo-face-register-10','e0000000-0000-4000-8000-000000000010','REGISTER',1,'SUCCEEDED',NULL,'2026-07-25 09:30:00.000000','2026-07-25 09:30:40.000000','2026-07-25 09:30:00.000000','2026-07-25 09:30:00.000000');
/*!40000 ALTER TABLE `face_profile_sync_operation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (1,'1','create billisan schema','SQL','V1__create_billisan_schema.sql',-243557392,'billisan','2026-08-08 04:27:59',375,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (2,'2','add idempotency and concurrency constraints','SQL','V2__add_idempotency_and_concurrency_constraints.sql',-2062825448,'billisan','2026-08-08 04:27:59',441,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (3,'3','align return slot authority','SQL','V3__align_return_slot_authority.sql',86275445,'billisan','2026-08-08 04:27:59',66,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (4,'4','add damage settlement basis','SQL','V4__add_damage_settlement_basis.sql',435107315,'billisan','2026-08-08 04:27:59',55,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (5,'5','add amount and time safety','SQL','V5__add_amount_and_time_safety.sql',742242089,'billisan','2026-08-08 04:27:59',336,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (6,'6','add rental failure summary','SQL','V6__add_rental_failure_summary.sql',1236997819,'billisan','2026-08-08 04:27:59',67,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (7,'7','add damage inspection processing time','SQL','V7__add_damage_inspection_processing_time.sql',726900449,'billisan','2026-08-08 04:27:59',60,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (8,'8','standardize uuid and idempotency key types','SQL','V8__standardize_uuid_and_idempotency_key_types.sql',2045603940,'billisan','2026-08-08 04:27:59',380,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (9,'9','standardize varchar and enum storage','SQL','V9__standardize_varchar_and_enum_storage.sql',-1700132118,'billisan','2026-08-08 04:27:59',378,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (10,'10','add ai and amount consistency checks','SQL','V10__add_ai_and_amount_consistency_checks.sql',677673367,'billisan','2026-08-08 04:27:59',86,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (11,'11','map internal and toss payment statuses','SQL','V11__map_internal_and_toss_payment_statuses.sql',-1171762697,'billisan','2026-08-08 04:27:59',53,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (12,'12','replace deprecated binary payment status cast','SQL','V12__replace_deprecated_binary_payment_status_cast.sql',-2027411477,'billisan','2026-08-08 04:27:59',45,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (13,'13','use student number and opaque user ref','SQL','V13__use_student_number_and_opaque_user_ref.sql',1643110306,'billisan','2026-08-08 04:27:59',189,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (14,'14','enforce cross flow integrity','SQL','V14__enforce_cross_flow_integrity.sql',1550585450,'billisan','2026-08-08 04:27:59',298,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (15,'15','block nested student identity evidence keys','SQL','V15__block_nested_student_identity_evidence_keys.sql',640440994,'billisan','2026-08-08 04:27:59',45,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (16,'16','protect payment ledger and settlement totals','SQL','V16__protect_payment_ledger_and_settlement_totals.sql',-430238915,'billisan','2026-08-08 04:27:59',48,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (17,'17','freeze settled damage review audit','SQL','V17__freeze_settled_damage_review_audit.sql',-818184942,'billisan','2026-08-08 04:27:59',16,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (18,'18','make damage settlement basis irreversible','SQL','V18__make_damage_settlement_basis_irreversible.sql',-1618444835,'billisan','2026-08-08 04:27:59',22,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (19,'19','add user rental eligibility projection','SQL','V19__add_user_rental_eligibility_projection.sql',860217627,'billisan','2026-08-08 04:27:59',37,1);
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (20,'20','align rental due at time order','SQL','V20__align_rental_due_at_time_order.sql',-205959859,'billisan','2026-08-08 04:27:59',44,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_attempt`
--

DROP TABLE IF EXISTS `payment_attempt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_attempt` (
  `payment_attempt_id` char(36) NOT NULL,
  `settlement_id` char(36) NOT NULL,
  `creation_request_id` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Opaque case-sensitive idempotency key',
  `toss_order_id` varchar(100) DEFAULT NULL,
  `toss_payment_key` varchar(200) DEFAULT NULL,
  `amount` bigint unsigned NOT NULL,
  `provider` varchar(50) NOT NULL,
  `toss_status` varchar(50) DEFAULT NULL,
  `failure_code` varchar(100) DEFAULT NULL,
  `failure_message` varchar(500) DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  `requested_at` datetime(6) NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`payment_attempt_id`),
  UNIQUE KEY `UK_PAYMENT_CREATION_REQUEST_ID` (`creation_request_id`),
  UNIQUE KEY `UK_PAYMENT_TOSS_ORDER_ID` (`toss_order_id`),
  UNIQUE KEY `UK_PAYMENT_TOSS_PAYMENT_KEY` (`toss_payment_key`),
  KEY `FK_PAYMENT_ATTEMPT_SETTLEMENT` (`settlement_id`),
  CONSTRAINT `FK_PAYMENT_ATTEMPT_SETTLEMENT` FOREIGN KEY (`settlement_id`) REFERENCES `settlement` (`settlement_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `CK_PAYMENT_AMOUNT` CHECK ((`amount` >= 0)),
  CONSTRAINT `CK_PAYMENT_ATTEMPT_AMOUNT_RANGE` CHECK ((`amount` between 1 and 7000)),
  CONSTRAINT `CK_PAYMENT_ATTEMPT_AUDIT_TIME_ORDER` CHECK ((`updated_at` >= `created_at`)),
  CONSTRAINT `CK_PAYMENT_ATTEMPT_ENUM_STORAGE` CHECK ((regexp_like(`provider`,_utf8mb4'^[A-Z][A-Z0-9_]{0,49}$',_utf8mb4'c') and regexp_like(`status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c'))),
  CONSTRAINT `CK_PAYMENT_ATTEMPT_IDEMPOTENCY_KEY_SHAPE` CHECK (((char_length(trim(`creation_request_id`)) between 1 and 100) and (length(`creation_request_id`) = length(trim(`creation_request_id`))))),
  CONSTRAINT `CK_PAYMENT_ATTEMPT_LIFECYCLE_TIME_ORDER` CHECK ((((`approved_at` is null) or (`approved_at` >= `requested_at`)) and ((`completed_at` is null) or (`completed_at` >= `requested_at`)) and ((`approved_at` is null) or (`completed_at` is null) or (`completed_at` >= `approved_at`)))),
  CONSTRAINT `CK_PAYMENT_ATTEMPT_STATE_TIME_COMPLETENESS` CHECK ((((`status` = _utf8mb4'REQUESTED') and (`approved_at` is null) and (`completed_at` is null)) or ((`status` = _utf8mb4'PROCESSING') and (`completed_at` is null)) or ((`status` = _utf8mb4'SUCCEEDED') and (`approved_at` is not null) and (`completed_at` is not null)) or ((`status` in (_utf8mb4'FAILED',_utf8mb4'CANCELLED',_utf8mb4'RECONCILIATION_REQUIRED')) and (`completed_at` is not null)))),
  CONSTRAINT `CK_PAYMENT_PROVIDER` CHECK ((`provider` in (_utf8mb4'MOCK',_utf8mb4'TOSS_SANDBOX'))),
  CONSTRAINT `CK_PAYMENT_PROVIDER_STATUS_MAPPING` CHECK ((((`provider` = _utf8mb4'MOCK') and (`toss_status` is null) and (`status` <> _utf8mb4'RECONCILIATION_REQUIRED')) or ((`provider` = _utf8mb4'TOSS_SANDBOX') and (((`status` = _utf8mb4'REQUESTED') and (`toss_status` is null)) or ((`status` = _utf8mb4'PROCESSING') and (`toss_status` is not null) and (cast(`toss_status` as char charset binary) in (_utf8mb4'READY',_utf8mb4'IN_PROGRESS',_utf8mb4'WAITING_FOR_DEPOSIT'))) or ((`status` = _utf8mb4'SUCCEEDED') and (`toss_status` is not null) and (cast(`toss_status` as char charset binary) = _utf8mb4'DONE')) or ((`status` = _utf8mb4'FAILED') and ((`toss_status` is null) or (cast(`toss_status` as char charset binary) in (_utf8mb4'ABORTED',_utf8mb4'EXPIRED')))) or ((`status` = _utf8mb4'CANCELLED') and ((`toss_status` is null) or (cast(`toss_status` as char charset binary) = _utf8mb4'CANCELED'))) or ((`status` = _utf8mb4'RECONCILIATION_REQUIRED') and (`toss_status` is not null) and (char_length(trim(`toss_status`)) between 1 and 50) and (length(`toss_status`) = length(trim(`toss_status`)))))))),
  CONSTRAINT `CK_PAYMENT_STATUS` CHECK ((`status` in (_utf8mb4'REQUESTED',_utf8mb4'PROCESSING',_utf8mb4'SUCCEEDED',_utf8mb4'FAILED',_utf8mb4'CANCELLED',_utf8mb4'RECONCILIATION_REQUIRED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_attempt`
--

LOCK TABLES `payment_attempt` WRITE;
/*!40000 ALTER TABLE `payment_attempt` DISABLE KEYS */;
INSERT INTO `payment_attempt` (`payment_attempt_id`, `settlement_id`, `creation_request_id`, `toss_order_id`, `toss_payment_key`, `amount`, `provider`, `toss_status`, `failure_code`, `failure_message`, `status`, `requested_at`, `approved_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('78000000-0000-4000-8000-000000000002','77000000-0000-4000-8000-000000000004','fixture-payment-completed-success','fixture-toss-order-completed-success','fixture-toss-key-completed-success',4000,'TOSS_SANDBOX','DONE',NULL,NULL,'SUCCEEDED','2026-08-03 05:15:00.000000','2026-08-03 05:16:00.000000','2026-08-03 05:16:01.000000','2026-08-03 05:15:00.000000','2026-08-03 05:16:01.000000');
INSERT INTO `payment_attempt` (`payment_attempt_id`, `settlement_id`, `creation_request_id`, `toss_order_id`, `toss_payment_key`, `amount`, `provider`, `toss_status`, `failure_code`, `failure_message`, `status`, `requested_at`, `approved_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('78000000-0000-4000-8000-000000000005','77000000-0000-4000-8000-000000000005','fixture-payment-damage-success','fixture-toss-order-damage-success','fixture-toss-key-damage-success',7000,'TOSS_SANDBOX','DONE',NULL,NULL,'SUCCEEDED','2026-08-03 05:10:00.000000','2026-08-03 05:11:00.000000','2026-08-03 05:11:01.000000','2026-08-03 05:10:00.000000','2026-08-03 05:11:01.000000');
INSERT INTO `payment_attempt` (`payment_attempt_id`, `settlement_id`, `creation_request_id`, `toss_order_id`, `toss_payment_key`, `amount`, `provider`, `toss_status`, `failure_code`, `failure_message`, `status`, `requested_at`, `approved_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('78000000-0000-4000-8000-000000000006','77000000-0000-4000-8000-000000000006','fixture-payment-overdue-success','fixture-toss-order-overdue-success','fixture-toss-key-overdue-success',1000,'TOSS_SANDBOX','DONE',NULL,NULL,'SUCCEEDED','2026-08-03 05:12:00.000000','2026-08-03 05:13:00.000000','2026-08-03 05:13:01.000000','2026-08-03 05:12:00.000000','2026-08-03 05:13:01.000000');
INSERT INTO `payment_attempt` (`payment_attempt_id`, `settlement_id`, `creation_request_id`, `toss_order_id`, `toss_payment_key`, `amount`, `provider`, `toss_status`, `failure_code`, `failure_message`, `status`, `requested_at`, `approved_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('78000000-0000-4000-8000-000000000007','77000000-0000-4000-8000-000000000007','fixture-payment-baseline-failed','fixture-toss-order-baseline-failed','fixture-toss-key-baseline-failed',2000,'TOSS_SANDBOX','ABORTED','FIXTURE_PAYMENT_DECLINED','Fixture payment was rejected','FAILED','2026-08-03 05:20:00.000000',NULL,'2026-08-03 05:20:30.000000','2026-08-03 05:20:00.000000','2026-08-03 05:20:30.000000');
INSERT INTO `payment_attempt` (`payment_attempt_id`, `settlement_id`, `creation_request_id`, `toss_order_id`, `toss_payment_key`, `amount`, `provider`, `toss_status`, `failure_code`, `failure_message`, `status`, `requested_at`, `approved_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('78000000-0000-4000-8000-000000000008','77000000-0000-4000-8000-000000000007','fixture-payment-after-failure-success','fixture-toss-order-after-failure-success','fixture-toss-key-after-failure-success',2000,'TOSS_SANDBOX','DONE',NULL,NULL,'SUCCEEDED','2026-08-03 05:22:00.000000','2026-08-03 05:23:00.000000','2026-08-03 05:23:01.000000','2026-08-03 05:22:00.000000','2026-08-03 05:23:01.000000');
INSERT INTO `payment_attempt` (`payment_attempt_id`, `settlement_id`, `creation_request_id`, `toss_order_id`, `toss_payment_key`, `amount`, `provider`, `toss_status`, `failure_code`, `failure_message`, `status`, `requested_at`, `approved_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('e9000000-0000-4000-8000-000000000087','e7000000-0000-4000-8000-000000000080','demo-payment-0087','demo-order-0087','demo-key-0087',2000,'TOSS_SANDBOX','DONE',NULL,NULL,'SUCCEEDED','2026-07-30 14:17:00.000000','2026-07-30 14:17:20.000000','2026-07-30 14:17:25.000000','2026-07-30 14:17:00.000000','2026-07-30 14:17:25.000000');
INSERT INTO `payment_attempt` (`payment_attempt_id`, `settlement_id`, `creation_request_id`, `toss_order_id`, `toss_payment_key`, `amount`, `provider`, `toss_status`, `failure_code`, `failure_message`, `status`, `requested_at`, `approved_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('e9000000-0000-4000-8000-000000000088','e7000000-0000-4000-8000-000000000081','demo-payment-0088','demo-order-0088','demo-key-0088',2000,'TOSS_SANDBOX','DONE',NULL,NULL,'SUCCEEDED','2026-08-06 17:17:00.000000','2026-08-06 17:17:20.000000','2026-08-06 17:17:25.000000','2026-08-06 17:17:00.000000','2026-08-06 17:17:25.000000');
INSERT INTO `payment_attempt` (`payment_attempt_id`, `settlement_id`, `creation_request_id`, `toss_order_id`, `toss_payment_key`, `amount`, `provider`, `toss_status`, `failure_code`, `failure_message`, `status`, `requested_at`, `approved_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('e9000000-0000-4000-8000-000000000089','e7000000-0000-4000-8000-000000000082','demo-payment-0089','demo-order-0089','demo-key-0089',7000,'TOSS_SANDBOX','DONE',NULL,NULL,'SUCCEEDED','2026-08-07 04:14:09.000000','2026-08-07 04:14:29.000000','2026-08-07 04:14:34.000000','2026-08-07 04:14:09.000000','2026-08-07 04:14:34.000000');
INSERT INTO `payment_attempt` (`payment_attempt_id`, `settlement_id`, `creation_request_id`, `toss_order_id`, `toss_payment_key`, `amount`, `provider`, `toss_status`, `failure_code`, `failure_message`, `status`, `requested_at`, `approved_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('e9000000-0000-4000-8000-000000000090','e7000000-0000-4000-8000-000000000083','demo-payment-0090','demo-order-0090','demo-key-0090',7000,'TOSS_SANDBOX','DONE',NULL,NULL,'SUCCEEDED','2026-07-26 07:14:09.000000','2026-07-26 07:14:29.000000','2026-07-26 07:14:34.000000','2026-07-26 07:14:09.000000','2026-07-26 07:14:34.000000');
INSERT INTO `payment_attempt` (`payment_attempt_id`, `settlement_id`, `creation_request_id`, `toss_order_id`, `toss_payment_key`, `amount`, `provider`, `toss_status`, `failure_code`, `failure_message`, `status`, `requested_at`, `approved_at`, `completed_at`, `created_at`, `updated_at`) VALUES ('e9000000-0000-4000-8000-000000000091','e7000000-0000-4000-8000-000000000084','demo-payment-0091','demo-order-0091','demo-key-0091',7000,'TOSS_SANDBOX','DONE',NULL,NULL,'SUCCEEDED','2026-07-27 10:14:09.000000','2026-07-27 10:14:29.000000','2026-07-27 10:14:34.000000','2026-07-27 10:14:09.000000','2026-07-27 10:14:34.000000');
/*!40000 ALTER TABLE `payment_attempt` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `TRG_PAYMENT_ATTEMPT_CREDIT_INSERT` BEFORE INSERT ON `payment_attempt` FOR EACH ROW BEGIN
	DECLARE `settlement_amount_value` BIGINT UNSIGNED DEFAULT NULL;
	DECLARE `settlement_paid_amount_value` BIGINT UNSIGNED DEFAULT NULL;
	DECLARE `settlement_status_value` VARCHAR(30) DEFAULT NULL;
	DECLARE `next_paid_amount_value` BIGINT UNSIGNED DEFAULT NULL;
	DECLARE `next_status_value` VARCHAR(30) DEFAULT NULL;
	DECLARE `next_paid_at_value` DATETIME(6) DEFAULT NULL;
	DECLARE CONTINUE HANDLER FOR NOT FOUND
		SET `settlement_amount_value` = NULL;

	IF `NEW`.`status` = 'SUCCEEDED' THEN
		IF `NEW`.`approved_at` IS NULL
			OR `NEW`.`completed_at` IS NULL THEN
			SIGNAL SQLSTATE '45000'
				SET MESSAGE_TEXT = 'SUCCEEDED_PAYMENT_REQUIRES_TERMINAL_TIMES';
		END IF;

		SELECT `amount`, `paid_amount`, `status`
		INTO
			`settlement_amount_value`,
			`settlement_paid_amount_value`,
			`settlement_status_value`
		FROM `settlement`
		WHERE `settlement_id` = `NEW`.`settlement_id`
		FOR UPDATE;

		IF `settlement_amount_value` IS NULL
			OR `settlement_status_value` <> 'PENDING'
			OR `settlement_paid_amount_value` + `NEW`.`amount`
				> `settlement_amount_value` THEN
			SIGNAL SQLSTATE '45000'
				SET MESSAGE_TEXT = 'PAYMENT_EXCEEDS_OUTSTANDING_BALANCE';
		END IF;

		SET `next_paid_amount_value` =
			`settlement_paid_amount_value` + `NEW`.`amount`;
		SET `next_status_value` = CASE
			WHEN `next_paid_amount_value` = `settlement_amount_value`
				THEN 'PAID'
			ELSE 'PENDING'
		END;
		SET `next_paid_at_value` = CASE
			WHEN `next_status_value` = 'PAID' THEN `NEW`.`approved_at`
			ELSE NULL
		END;

		INSERT INTO `settlement_payment_mutation_guard` (
			`connection_id`,
			`settlement_id`,
			`expected_paid_amount`,
			`expected_status`,
			`expected_paid_at`
		) VALUES (
			CONNECTION_ID(),
			`NEW`.`settlement_id`,
			`next_paid_amount_value`,
			`next_status_value`,
			`next_paid_at_value`
		);

		UPDATE `settlement`
		SET
			`paid_amount` = `next_paid_amount_value`,
			`status` = `next_status_value`,
			`paid_at` = `next_paid_at_value`
		WHERE `settlement_id` = `NEW`.`settlement_id`;

		IF ROW_COUNT() <> 1 THEN
			SIGNAL SQLSTATE '45000'
				SET MESSAGE_TEXT = 'PAYMENT_SETTLEMENT_UPDATE_CONFLICT';
		END IF;
	END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `TRG_PAYMENT_ATTEMPT_CREDIT_UPDATE` BEFORE UPDATE ON `payment_attempt` FOR EACH ROW BEGIN
	DECLARE `settlement_amount_value` BIGINT UNSIGNED DEFAULT NULL;
	DECLARE `settlement_paid_amount_value` BIGINT UNSIGNED DEFAULT NULL;
	DECLARE `settlement_status_value` VARCHAR(30) DEFAULT NULL;
	DECLARE `next_paid_amount_value` BIGINT UNSIGNED DEFAULT NULL;
	DECLARE `next_status_value` VARCHAR(30) DEFAULT NULL;
	DECLARE `next_paid_at_value` DATETIME(6) DEFAULT NULL;
	DECLARE CONTINUE HANDLER FOR NOT FOUND
		SET `settlement_amount_value` = NULL;

	IF `NEW`.`settlement_id` <> `OLD`.`settlement_id`
		OR `NEW`.`amount` <> `OLD`.`amount` THEN
		SIGNAL SQLSTATE '45000'
			SET MESSAGE_TEXT = 'PAYMENT_ALLOCATION_IMMUTABLE';
	END IF;

	IF `OLD`.`status` = 'SUCCEEDED'
		AND (
			`NEW`.`status` <> 'SUCCEEDED'
			OR NOT (`NEW`.`approved_at` <=> `OLD`.`approved_at`)
			OR NOT (`NEW`.`completed_at` <=> `OLD`.`completed_at`)
		) THEN
		SIGNAL SQLSTATE '45000'
			SET MESSAGE_TEXT = 'SUCCEEDED_PAYMENT_IS_TERMINAL';
	END IF;

	IF `OLD`.`status` <> 'SUCCEEDED'
		AND `NEW`.`status` = 'SUCCEEDED' THEN
		IF `NEW`.`approved_at` IS NULL
			OR `NEW`.`completed_at` IS NULL THEN
			SIGNAL SQLSTATE '45000'
				SET MESSAGE_TEXT = 'SUCCEEDED_PAYMENT_REQUIRES_TERMINAL_TIMES';
		END IF;

		SELECT `amount`, `paid_amount`, `status`
		INTO
			`settlement_amount_value`,
			`settlement_paid_amount_value`,
			`settlement_status_value`
		FROM `settlement`
		WHERE `settlement_id` = `NEW`.`settlement_id`
		FOR UPDATE;

		IF `settlement_amount_value` IS NULL
			OR `settlement_status_value` <> 'PENDING'
			OR `settlement_paid_amount_value` + `NEW`.`amount`
				> `settlement_amount_value` THEN
			SIGNAL SQLSTATE '45000'
				SET MESSAGE_TEXT = 'PAYMENT_EXCEEDS_OUTSTANDING_BALANCE';
		END IF;

		SET `next_paid_amount_value` =
			`settlement_paid_amount_value` + `NEW`.`amount`;
		SET `next_status_value` = CASE
			WHEN `next_paid_amount_value` = `settlement_amount_value`
				THEN 'PAID'
			ELSE 'PENDING'
		END;
		SET `next_paid_at_value` = CASE
			WHEN `next_status_value` = 'PAID' THEN `NEW`.`approved_at`
			ELSE NULL
		END;

		INSERT INTO `settlement_payment_mutation_guard` (
			`connection_id`,
			`settlement_id`,
			`expected_paid_amount`,
			`expected_status`,
			`expected_paid_at`
		) VALUES (
			CONNECTION_ID(),
			`NEW`.`settlement_id`,
			`next_paid_amount_value`,
			`next_status_value`,
			`next_paid_at_value`
		);

		UPDATE `settlement`
		SET
			`paid_amount` = `next_paid_amount_value`,
			`status` = `next_status_value`,
			`paid_at` = `next_paid_at_value`
		WHERE `settlement_id` = `NEW`.`settlement_id`;

		IF ROW_COUNT() <> 1 THEN
			SIGNAL SQLSTATE '45000'
				SET MESSAGE_TEXT = 'PAYMENT_SETTLEMENT_UPDATE_CONFLICT';
		END IF;
	END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `TRG_PAYMENT_ATTEMPT_BEFORE_DELETE` BEFORE DELETE ON `payment_attempt` FOR EACH ROW BEGIN
	IF `OLD`.`status` = 'SUCCEEDED' THEN
		SIGNAL SQLSTATE '45000'
			SET MESSAGE_TEXT = 'SUCCEEDED_PAYMENT_DELETE_FORBIDDEN';
	END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `rental`
--

DROP TABLE IF EXISTS `rental`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rental` (
  `rental_id` char(36) NOT NULL,
  `user_id` char(9) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `checkout_slot_id` char(36) NOT NULL,
  `rental_request_id` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Opaque case-sensitive idempotency key',
  `status` varchar(30) NOT NULL,
  `failure_code` varchar(100) DEFAULT NULL COMMENT 'Stable business failure code; never a raw exception type',
  `failure_message` varchar(500) DEFAULT NULL COMMENT 'Optional sanitized summary; no PII, stack trace, raw exception, or full MQTT payload',
  `failed_at` datetime(6) DEFAULT NULL,
  `requested_at` datetime(6) NOT NULL,
  `rented_at` datetime(6) DEFAULT NULL,
  `due_at` datetime(6) DEFAULT NULL,
  `ended_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `active_user_guard` char(9) CHARACTER SET ascii COLLATE ascii_bin GENERATED ALWAYS AS ((case when (`status` in (_utf8mb4'REQUESTED',_utf8mb4'ACTIVE',_utf8mb4'RETURNING')) then `user_id` else NULL end)) STORED,
  `requested_slot_guard` char(36) GENERATED ALWAYS AS ((case when (`status` = _utf8mb4'REQUESTED') then `checkout_slot_id` else NULL end)) STORED,
  PRIMARY KEY (`rental_id`),
  UNIQUE KEY `UK_RENTAL_REQUEST_ID` (`rental_request_id`),
  UNIQUE KEY `UK_RENTAL_ID_USER_ID` (`rental_id`,`user_id`),
  UNIQUE KEY `UK_RENTAL_REQUESTED_SLOT_GUARD` (`requested_slot_guard`),
  UNIQUE KEY `UK_RENTAL_ACTIVE_USER_GUARD` (`active_user_guard`),
  KEY `FK_RENTAL_CHECKOUT_SLOT` (`checkout_slot_id`),
  KEY `FK_RENTAL_USER` (`user_id`),
  CONSTRAINT `FK_RENTAL_CHECKOUT_SLOT` FOREIGN KEY (`checkout_slot_id`) REFERENCES `slot` (`slot_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FK_RENTAL_USER` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `CK_RENTAL_AUDIT_TIME_ORDER` CHECK ((`updated_at` >= `created_at`)),
  CONSTRAINT `CK_RENTAL_ENUM_STORAGE` CHECK (regexp_like(`status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c')),
  CONSTRAINT `CK_RENTAL_FAILURE_CODE` CHECK (((`failure_code` is null) or (cast(`failure_code` as char charset binary) in (_utf8mb4'AUTHENTICATION_FAILED',_utf8mb4'USER_NOT_ELIGIBLE',_utf8mb4'SLOT_NOT_AVAILABLE',_utf8mb4'SLOT_CONFLICT',_utf8mb4'MQTT_BROKER_UNAVAILABLE',_utf8mb4'DEVICE_OFFLINE',_utf8mb4'DEVICE_COMMAND_TIMEOUT',_utf8mb4'UNLOCK_FAILED',_utf8mb4'PHYSICAL_REMOVAL_NOT_DETECTED',_utf8mb4'STATE_RECONCILIATION_REQUIRED',_utf8mb4'INTERNAL_ERROR',_utf8mb4'LEGACY_UNSPECIFIED')))),
  CONSTRAINT `CK_RENTAL_FAILURE_STATE` CHECK ((((`status` = _utf8mb4'FAILED') and (`failure_code` is not null) and (`failed_at` is not null)) or ((`status` <> _utf8mb4'FAILED') and (`failure_code` is null) and (`failure_message` is null) and (`failed_at` is null)))),
  CONSTRAINT `CK_RENTAL_FAILURE_TIME_ORDER` CHECK (((`failed_at` is null) or (`failed_at` >= `requested_at`))),
  CONSTRAINT `CK_RENTAL_IDEMPOTENCY_KEY_SHAPE` CHECK (((char_length(trim(`rental_request_id`)) between 1 and 100) and (length(`rental_request_id`) = length(trim(`rental_request_id`))))),
  CONSTRAINT `CK_RENTAL_LIFECYCLE_TIME_ORDER` CHECK ((((`rented_at` is null) or (`rented_at` >= `requested_at`)) and ((`due_at` is null) or ((`rented_at` is not null) and (`due_at` > `rented_at`))) and ((`ended_at` is null) or ((`rented_at` is not null) and (`ended_at` >= `rented_at`))))),
  CONSTRAINT `CK_RENTAL_STATE_TIME_COMPLETENESS` CHECK ((((`status` = _utf8mb4'REQUESTED') and (`rented_at` is null) and (`due_at` is null) and (`ended_at` is null)) or ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'RETURNING')) and (`rented_at` is not null) and (`due_at` is not null) and (`ended_at` is null)) or ((`status` in (_utf8mb4'COMPLETED',_utf8mb4'LOST')) and (`rented_at` is not null) and (`due_at` is not null) and (`ended_at` is not null)) or (`status` in (_utf8mb4'CANCELLED',_utf8mb4'FAILED')))),
  CONSTRAINT `CK_RENTAL_STATUS` CHECK ((`status` in (_utf8mb4'REQUESTED',_utf8mb4'ACTIVE',_utf8mb4'RETURNING',_utf8mb4'COMPLETED',_utf8mb4'LOST',_utf8mb4'CANCELLED',_utf8mb4'FAILED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rental`
--

LOCK TABLES `rental` WRITE;
/*!40000 ALTER TABLE `rental` DISABLE KEYS */;
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('44000000-0000-4000-8000-000000000003','202600005','33000000-0000-4000-8000-000000000004','fixture-rental-unpaid-settlement','COMPLETED',NULL,NULL,NULL,'2026-08-03 03:00:00.000000','2026-08-03 03:01:00.000000','2026-08-04 03:01:00.000000','2026-08-03 03:30:00.000000','2026-08-03 03:00:00.000000','2026-08-03 03:40:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('44000000-0000-4000-8000-000000000004','202600001','33000000-0000-4000-8000-000000000001','fixture-rental-normal-completed','COMPLETED',NULL,NULL,NULL,'2026-08-03 00:10:00.000000','2026-08-03 00:11:00.000000','2026-08-04 00:11:00.000000','2026-08-03 00:30:00.000000','2026-08-03 00:10:00.000000','2026-08-03 00:35:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('44000000-0000-4000-8000-000000000005','202600007','33000000-0000-4000-8000-000000000002','fixture-rental-face-ineligible','FAILED','USER_NOT_ELIGIBLE','Fixture user has no registered face profile','2026-08-03 01:30:05.000000','2026-08-03 01:30:00.000000',NULL,NULL,NULL,'2026-08-03 01:30:00.000000','2026-08-03 01:30:05.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('44000000-0000-4000-8000-000000000006','202600006','33000000-0000-4000-8000-000000000002','fixture-rental-ai-failed','COMPLETED',NULL,NULL,NULL,'2026-08-03 01:35:00.000000','2026-08-03 01:36:00.000000','2026-08-04 01:36:00.000000','2026-08-03 01:50:00.000000','2026-08-03 01:35:00.000000','2026-08-03 01:52:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('44000000-0000-4000-8000-000000000007','202600002','33000000-0000-4000-8000-000000000004','fixture-rental-payment-partial','COMPLETED',NULL,NULL,NULL,'2026-08-01 04:00:00.000000','2026-08-01 04:01:00.000000','2026-08-02 04:01:00.000000','2026-08-03 04:30:00.000000','2026-08-01 04:00:00.000000','2026-08-03 04:30:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('44000000-0000-4000-8000-000000000008','202600004','33000000-0000-4000-8000-000000000003','fixture-rental-review-pending-completed','COMPLETED',NULL,NULL,NULL,'2026-08-03 02:00:00.000000','2026-08-03 02:01:00.000000','2026-08-04 02:01:00.000000','2026-08-03 02:20:00.000000','2026-08-03 02:00:00.000000','2026-08-03 02:20:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('44000000-0000-4000-8000-000000000009','202600003','33000000-0000-4000-8000-000000000004','fixture-rental-overdue-completed','COMPLETED',NULL,NULL,NULL,'2026-08-01 01:00:00.000000','2026-08-01 01:01:00.000000','2026-08-02 01:01:00.000000','2026-08-03 04:50:00.000000','2026-08-01 01:00:00.000000','2026-08-03 04:50:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000001','202512001','e3000000-0000-4000-8000-000000000001','demo-rental-0001','COMPLETED',NULL,NULL,NULL,'2026-07-25 09:00:00.000000','2026-07-25 09:02:00.000000','2026-07-26 09:02:00.000000','2026-07-26 03:03:00.000000','2026-07-25 09:00:00.000000','2026-07-26 03:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000003','202512002','e3000000-0000-4000-8000-000000000002','demo-rental-0003','COMPLETED',NULL,NULL,NULL,'2026-07-26 12:00:00.000000','2026-07-26 12:02:00.000000','2026-07-27 12:02:00.000000','2026-07-27 06:03:00.000000','2026-07-26 12:00:00.000000','2026-07-27 06:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000005','202512003','e3000000-0000-4000-8000-000000000003','demo-rental-0005','COMPLETED',NULL,NULL,NULL,'2026-07-27 15:00:00.000000','2026-07-27 15:02:00.000000','2026-07-28 15:02:00.000000','2026-07-28 09:03:00.000000','2026-07-27 15:00:00.000000','2026-07-28 09:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000007','202512004','e3000000-0000-4000-8000-000000000004','demo-rental-0007','COMPLETED',NULL,NULL,NULL,'2026-07-28 09:00:00.000000','2026-07-28 09:02:00.000000','2026-07-29 09:02:00.000000','2026-07-30 14:03:00.000000','2026-07-28 09:00:00.000000','2026-07-30 14:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000009','202512005','e3000000-0000-4000-8000-000000000005','demo-rental-0009','COMPLETED',NULL,NULL,NULL,'2026-07-29 12:00:00.000000','2026-07-29 12:02:00.000000','2026-07-30 12:02:00.000000','2026-07-30 06:03:00.000000','2026-07-29 12:00:00.000000','2026-07-30 06:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000011','202512006','e3000000-0000-4000-8000-000000000006','demo-rental-0011','COMPLETED',NULL,NULL,NULL,'2026-07-30 15:00:00.000000','2026-07-30 15:02:00.000000','2026-07-31 15:02:00.000000','2026-07-31 09:03:00.000000','2026-07-30 15:00:00.000000','2026-07-31 09:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000013','202512007','e3000000-0000-4000-8000-000000000007','demo-rental-0013','COMPLETED',NULL,NULL,NULL,'2026-07-31 09:00:00.000000','2026-07-31 09:02:00.000000','2026-08-01 09:02:00.000000','2026-08-01 03:03:00.000000','2026-07-31 09:00:00.000000','2026-08-01 03:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000015','202512008','e3000000-0000-4000-8000-000000000008','demo-rental-0015','COMPLETED',NULL,NULL,NULL,'2026-08-01 12:00:00.000000','2026-08-01 12:02:00.000000','2026-08-02 12:02:00.000000','2026-08-02 06:03:00.000000','2026-08-01 12:00:00.000000','2026-08-02 06:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000017','202512009','e3000000-0000-4000-8000-000000000009','demo-rental-0017','COMPLETED',NULL,NULL,NULL,'2026-08-02 15:00:00.000000','2026-08-02 15:02:00.000000','2026-08-03 15:02:00.000000','2026-08-03 09:03:00.000000','2026-08-02 15:00:00.000000','2026-08-03 09:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000019','202512010','e3000000-0000-4000-8000-000000000010','demo-rental-0019','COMPLETED',NULL,NULL,NULL,'2026-08-03 09:00:00.000000','2026-08-03 09:02:00.000000','2026-08-04 09:02:00.000000','2026-08-04 03:03:00.000000','2026-08-03 09:00:00.000000','2026-08-04 03:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000021','202512011','e3000000-0000-4000-8000-000000000001','demo-rental-0021','COMPLETED',NULL,NULL,NULL,'2026-08-04 12:00:00.000000','2026-08-04 12:02:00.000000','2026-08-05 12:02:00.000000','2026-08-06 17:03:00.000000','2026-08-04 12:00:00.000000','2026-08-06 17:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000023','202512012','e3000000-0000-4000-8000-000000000002','demo-rental-0023','COMPLETED',NULL,NULL,NULL,'2026-08-05 15:00:00.000000','2026-08-05 15:02:00.000000','2026-08-06 15:02:00.000000','2026-08-06 09:03:00.000000','2026-08-05 15:00:00.000000','2026-08-06 09:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000025','202512001','e3000000-0000-4000-8000-000000000003','demo-rental-0025','COMPLETED',NULL,NULL,NULL,'2026-08-06 09:00:00.000000','2026-08-06 09:02:00.000000','2026-08-07 09:02:00.000000','2026-08-07 03:03:00.000000','2026-08-06 09:00:00.000000','2026-08-07 03:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000027','202512002','e3000000-0000-4000-8000-000000000004','demo-rental-0027','COMPLETED',NULL,NULL,NULL,'2026-07-25 12:00:00.000000','2026-07-25 12:02:00.000000','2026-07-26 12:02:00.000000','2026-07-26 06:03:00.000000','2026-07-25 12:00:00.000000','2026-07-26 06:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000029','202512003','e3000000-0000-4000-8000-000000000005','demo-rental-0029','COMPLETED',NULL,NULL,NULL,'2026-07-26 15:00:00.000000','2026-07-26 15:02:00.000000','2026-07-27 15:02:00.000000','2026-07-27 09:03:00.000000','2026-07-26 15:00:00.000000','2026-07-27 09:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000031','202512004','e3000000-0000-4000-8000-000000000006','demo-rental-0031','COMPLETED',NULL,NULL,NULL,'2026-07-27 09:00:00.000000','2026-07-27 09:02:00.000000','2026-07-28 09:02:00.000000','2026-07-28 03:03:00.000000','2026-07-27 09:00:00.000000','2026-07-28 03:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000033','202512005','e3000000-0000-4000-8000-000000000007','demo-rental-0033','COMPLETED',NULL,NULL,NULL,'2026-07-28 12:00:00.000000','2026-07-28 12:02:00.000000','2026-07-29 12:02:00.000000','2026-07-29 06:03:00.000000','2026-07-28 12:00:00.000000','2026-07-29 06:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000035','202512006','e3000000-0000-4000-8000-000000000008','demo-rental-0035','COMPLETED',NULL,NULL,NULL,'2026-07-29 15:00:00.000000','2026-07-29 15:02:00.000000','2026-07-30 15:02:00.000000','2026-07-31 20:03:00.000000','2026-07-29 15:00:00.000000','2026-07-31 20:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000037','202512007','e3000000-0000-4000-8000-000000000009','demo-rental-0037','COMPLETED',NULL,NULL,NULL,'2026-07-30 09:00:00.000000','2026-07-30 09:02:00.000000','2026-07-31 09:02:00.000000','2026-07-31 03:03:00.000000','2026-07-30 09:00:00.000000','2026-07-31 03:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000039','202512008','e3000000-0000-4000-8000-000000000010','demo-rental-0039','COMPLETED',NULL,NULL,NULL,'2026-07-31 12:00:00.000000','2026-07-31 12:02:00.000000','2026-08-01 12:02:00.000000','2026-08-01 06:03:00.000000','2026-07-31 12:00:00.000000','2026-08-01 06:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000041','202512009','e3000000-0000-4000-8000-000000000001','demo-rental-0041','COMPLETED',NULL,NULL,NULL,'2026-08-01 15:00:00.000000','2026-08-01 15:02:00.000000','2026-08-02 15:02:00.000000','2026-08-02 09:03:00.000000','2026-08-01 15:00:00.000000','2026-08-02 09:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000043','202512010','e3000000-0000-4000-8000-000000000002','demo-rental-0043','COMPLETED',NULL,NULL,NULL,'2026-08-02 09:00:00.000000','2026-08-02 09:02:00.000000','2026-08-03 09:02:00.000000','2026-08-03 03:03:00.000000','2026-08-02 09:00:00.000000','2026-08-03 03:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000045','202512011','e3000000-0000-4000-8000-000000000003','demo-rental-0045','COMPLETED',NULL,NULL,NULL,'2026-08-03 12:00:00.000000','2026-08-03 12:02:00.000000','2026-08-04 12:02:00.000000','2026-08-04 06:03:00.000000','2026-08-03 12:00:00.000000','2026-08-04 06:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000047','202512012','e3000000-0000-4000-8000-000000000004','demo-rental-0047','COMPLETED',NULL,NULL,NULL,'2026-08-04 15:00:00.000000','2026-08-04 15:02:00.000000','2026-08-05 15:02:00.000000','2026-08-05 09:03:00.000000','2026-08-04 15:00:00.000000','2026-08-05 09:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000049','202512001','e3000000-0000-4000-8000-000000000005','demo-rental-0049','COMPLETED',NULL,NULL,NULL,'2026-08-05 09:00:00.000000','2026-08-05 09:02:00.000000','2026-08-06 09:02:00.000000','2026-08-07 14:03:00.000000','2026-08-05 09:00:00.000000','2026-08-07 14:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000051','202512002','e3000000-0000-4000-8000-000000000006','demo-rental-0051','COMPLETED',NULL,NULL,NULL,'2026-08-06 12:00:00.000000','2026-08-06 12:02:00.000000','2026-08-07 12:02:00.000000','2026-08-07 06:03:00.000000','2026-08-06 12:00:00.000000','2026-08-07 06:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000053','202512003','e3000000-0000-4000-8000-000000000007','demo-rental-0053','COMPLETED',NULL,NULL,NULL,'2026-07-25 15:00:00.000000','2026-07-25 15:02:00.000000','2026-07-26 15:02:00.000000','2026-07-26 09:03:00.000000','2026-07-25 15:00:00.000000','2026-07-26 09:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000055','202512004','e3000000-0000-4000-8000-000000000008','demo-rental-0055','COMPLETED',NULL,NULL,NULL,'2026-07-26 09:00:00.000000','2026-07-26 09:02:00.000000','2026-07-27 09:02:00.000000','2026-07-27 03:03:00.000000','2026-07-26 09:00:00.000000','2026-07-27 03:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000057','202512005','e3000000-0000-4000-8000-000000000009','demo-rental-0057','COMPLETED',NULL,NULL,NULL,'2026-07-27 12:00:00.000000','2026-07-27 12:02:00.000000','2026-07-28 12:02:00.000000','2026-07-28 06:03:00.000000','2026-07-27 12:00:00.000000','2026-07-28 06:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000059','202512006','e3000000-0000-4000-8000-000000000010','demo-rental-0059','COMPLETED',NULL,NULL,NULL,'2026-07-28 15:00:00.000000','2026-07-28 15:02:00.000000','2026-07-29 15:02:00.000000','2026-07-29 09:03:00.000000','2026-07-28 15:00:00.000000','2026-07-29 09:03:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000092','202512007','e3000000-0000-4000-8000-000000000003','demo-rental-0092','ACTIVE',NULL,NULL,NULL,'2026-08-07 15:00:00.000000','2026-08-07 15:01:00.000000','2026-08-08 15:01:00.000000',NULL,'2026-08-07 15:00:00.000000','2026-08-07 15:00:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000093','202512008','e3000000-0000-4000-8000-000000000004','demo-rental-0093','ACTIVE',NULL,NULL,NULL,'2026-08-07 16:00:00.000000','2026-08-07 16:01:00.000000','2026-08-08 16:01:00.000000',NULL,'2026-08-07 16:00:00.000000','2026-08-07 16:00:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000094','202512009','e3000000-0000-4000-8000-000000000005','demo-rental-0094','ACTIVE',NULL,NULL,NULL,'2026-08-07 17:00:00.000000','2026-08-07 17:01:00.000000','2026-08-08 17:01:00.000000',NULL,'2026-08-07 17:00:00.000000','2026-08-07 17:00:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000095','202512004','e3000000-0000-4000-8000-000000000008','demo-rental-0095','RETURNING',NULL,NULL,NULL,'2026-08-07 19:00:00.000000','2026-08-07 19:01:00.000000','2026-08-08 19:01:00.000000',NULL,'2026-08-07 19:00:00.000000','2026-08-07 19:00:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000097','202512011','e3000000-0000-4000-8000-000000000005','demo-rental-0097','FAILED','USER_NOT_ELIGIBLE','얼굴 미등록 사용자','2026-08-05 23:00:12.000000','2026-08-05 23:00:00.000000',NULL,NULL,NULL,'2026-08-05 23:00:00.000000','2026-08-05 23:00:00.000000');
INSERT INTO `rental` (`rental_id`, `user_id`, `checkout_slot_id`, `rental_request_id`, `status`, `failure_code`, `failure_message`, `failed_at`, `requested_at`, `rented_at`, `due_at`, `ended_at`, `created_at`, `updated_at`) VALUES ('e4000000-0000-4000-8000-000000000098','202512012','e3000000-0000-4000-8000-000000000005','demo-rental-0098','FAILED','DEVICE_COMMAND_TIMEOUT','SLOT 개방 응답 없음','2026-08-06 23:00:12.000000','2026-08-06 23:00:00.000000',NULL,NULL,NULL,'2026-08-06 23:00:00.000000','2026-08-06 23:00:00.000000');
/*!40000 ALTER TABLE `rental` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `return_attempt`
--

DROP TABLE IF EXISTS `return_attempt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `return_attempt` (
  `return_attempt_id` char(36) NOT NULL,
  `rental_id` char(36) NOT NULL,
  `return_slot_id` char(36) DEFAULT NULL,
  `request_id` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Opaque case-sensitive idempotency key',
  `status` varchar(30) NOT NULL,
  `physical_completed_at` datetime(6) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `failure_reason` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `claimed_rental_guard` char(36) GENERATED ALWAYS AS ((case when (`status` in (_utf8mb4'PROCESSING',_utf8mb4'PHYSICAL_DONE',_utf8mb4'RECOVERY_REQUIRED',_utf8mb4'COMPLETED')) then `rental_id` else NULL end)) STORED,
  `assigned_return_slot_guard` char(36) GENERATED ALWAYS AS ((case when ((`return_slot_id` is not null) and (`status` in (_utf8mb4'PROCESSING',_utf8mb4'PHYSICAL_DONE',_utf8mb4'RECOVERY_REQUIRED'))) then `return_slot_id` else NULL end)) STORED,
  PRIMARY KEY (`return_attempt_id`),
  UNIQUE KEY `UK_RETURN_ATTEMPT_REQUEST_ID` (`request_id`),
  UNIQUE KEY `UK_RETURN_ATTEMPT_ID_RENTAL_ID` (`return_attempt_id`,`rental_id`),
  UNIQUE KEY `UK_RETURN_ATTEMPT_ASSIGNED_SLOT_GUARD` (`assigned_return_slot_guard`),
  UNIQUE KEY `UK_RETURN_ATTEMPT_CLAIMED_RENTAL_GUARD` (`claimed_rental_guard`),
  KEY `FK_RETURN_ATTEMPT_RENTAL` (`rental_id`),
  KEY `FK_RETURN_ATTEMPT_RETURN_SLOT` (`return_slot_id`),
  CONSTRAINT `FK_RETURN_ATTEMPT_RENTAL` FOREIGN KEY (`rental_id`) REFERENCES `rental` (`rental_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FK_RETURN_ATTEMPT_RETURN_SLOT` FOREIGN KEY (`return_slot_id`) REFERENCES `slot` (`slot_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `CK_RETURN_ATTEMPT_AUDIT_TIME_ORDER` CHECK ((`updated_at` >= `created_at`)),
  CONSTRAINT `CK_RETURN_ATTEMPT_ENUM_STORAGE` CHECK (regexp_like(`status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c')),
  CONSTRAINT `CK_RETURN_ATTEMPT_IDEMPOTENCY_KEY_SHAPE` CHECK (((char_length(trim(`request_id`)) between 1 and 100) and (length(`request_id`) = length(trim(`request_id`))))),
  CONSTRAINT `CK_RETURN_ATTEMPT_LIFECYCLE_TIME_ORDER` CHECK ((((`physical_completed_at` is null) or (`physical_completed_at` >= `created_at`)) and ((`completed_at` is null) or ((`physical_completed_at` is not null) and (`completed_at` >= `physical_completed_at`))))),
  CONSTRAINT `CK_RETURN_ATTEMPT_REQUIRED_RETURN_SLOT` CHECK (((`status` not in (_utf8mb4'PHYSICAL_DONE',_utf8mb4'COMPLETED')) or (`return_slot_id` is not null))),
  CONSTRAINT `CK_RETURN_ATTEMPT_STATE_TIME_COMPLETENESS` CHECK ((((`status` = _utf8mb4'PROCESSING') and (`physical_completed_at` is null) and (`completed_at` is null)) or ((`status` = _utf8mb4'PHYSICAL_DONE') and (`physical_completed_at` is not null) and (`completed_at` is null)) or ((`status` = _utf8mb4'COMPLETED') and (`physical_completed_at` is not null) and (`completed_at` is not null)) or ((`status` in (_utf8mb4'RECOVERY_REQUIRED',_utf8mb4'FAILED')) and (`completed_at` is null)))),
  CONSTRAINT `CK_RETURN_ATTEMPT_STATUS` CHECK ((`status` in (_utf8mb4'PROCESSING',_utf8mb4'PHYSICAL_DONE',_utf8mb4'COMPLETED',_utf8mb4'RECOVERY_REQUIRED',_utf8mb4'FAILED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `return_attempt`
--

LOCK TABLES `return_attempt` WRITE;
/*!40000 ALTER TABLE `return_attempt` DISABLE KEYS */;
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('55000000-0000-4000-8000-000000000002','44000000-0000-4000-8000-000000000003','33000000-0000-4000-8000-000000000005','fixture-return-unpaid-completed','COMPLETED','2026-08-03 03:29:00.000000','2026-08-03 03:30:00.000000',NULL,'2026-08-03 03:25:00.000000','2026-08-03 03:30:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('55000000-0000-4000-8000-000000000003','44000000-0000-4000-8000-000000000004','33000000-0000-4000-8000-000000000001','fixture-return-normal-completed','COMPLETED','2026-08-03 00:29:00.000000','2026-08-03 00:30:00.000000',NULL,'2026-08-03 00:25:00.000000','2026-08-03 00:30:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('55000000-0000-4000-8000-000000000004','44000000-0000-4000-8000-000000000006','33000000-0000-4000-8000-000000000002','fixture-return-ai-failed-completed','COMPLETED','2026-08-03 01:49:00.000000','2026-08-03 01:50:00.000000',NULL,'2026-08-03 01:45:00.000000','2026-08-03 01:50:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('55000000-0000-4000-8000-000000000006','44000000-0000-4000-8000-000000000007','33000000-0000-4000-8000-000000000004','fixture-return-payment-partial','COMPLETED','2026-08-03 04:29:00.000000','2026-08-03 04:30:00.000000',NULL,'2026-08-03 04:25:00.000000','2026-08-03 04:30:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('55000000-0000-4000-8000-000000000007','44000000-0000-4000-8000-000000000008','33000000-0000-4000-8000-000000000003','fixture-return-review-pending-completed','COMPLETED','2026-08-03 02:19:00.000000','2026-08-03 02:20:00.000000',NULL,'2026-08-03 02:10:00.000000','2026-08-03 02:20:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('55000000-0000-4000-8000-000000000008','44000000-0000-4000-8000-000000000009','33000000-0000-4000-8000-000000000004','fixture-return-overdue-completed','COMPLETED','2026-08-03 04:49:00.000000','2026-08-03 04:50:00.000000',NULL,'2026-08-03 04:45:00.000000','2026-08-03 04:50:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000002','e4000000-0000-4000-8000-000000000001','e3000000-0000-4000-8000-000000000004','demo-return-0002','COMPLETED','2026-07-26 03:02:00.000000','2026-07-26 03:03:00.000000',NULL,'2026-07-26 03:00:00.000000','2026-07-26 03:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000004','e4000000-0000-4000-8000-000000000003','e3000000-0000-4000-8000-000000000005','demo-return-0004','COMPLETED','2026-07-27 06:02:00.000000','2026-07-27 06:03:00.000000',NULL,'2026-07-27 06:00:00.000000','2026-07-27 06:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000006','e4000000-0000-4000-8000-000000000005','e3000000-0000-4000-8000-000000000006','demo-return-0006','COMPLETED','2026-07-28 09:02:00.000000','2026-07-28 09:03:00.000000',NULL,'2026-07-28 09:00:00.000000','2026-07-28 09:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000008','e4000000-0000-4000-8000-000000000007','e3000000-0000-4000-8000-000000000007','demo-return-0008','COMPLETED','2026-07-30 14:02:00.000000','2026-07-30 14:03:00.000000',NULL,'2026-07-30 14:00:00.000000','2026-07-30 14:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000010','e4000000-0000-4000-8000-000000000009','e3000000-0000-4000-8000-000000000008','demo-return-0010','COMPLETED','2026-07-30 06:02:00.000000','2026-07-30 06:03:00.000000',NULL,'2026-07-30 06:00:00.000000','2026-07-30 06:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000012','e4000000-0000-4000-8000-000000000011','e3000000-0000-4000-8000-000000000009','demo-return-0012','COMPLETED','2026-07-31 09:02:00.000000','2026-07-31 09:03:00.000000',NULL,'2026-07-31 09:00:00.000000','2026-07-31 09:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000014','e4000000-0000-4000-8000-000000000013','e3000000-0000-4000-8000-000000000010','demo-return-0014','COMPLETED','2026-08-01 03:02:00.000000','2026-08-01 03:03:00.000000',NULL,'2026-08-01 03:00:00.000000','2026-08-01 03:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000016','e4000000-0000-4000-8000-000000000015','e3000000-0000-4000-8000-000000000001','demo-return-0016','COMPLETED','2026-08-02 06:02:00.000000','2026-08-02 06:03:00.000000',NULL,'2026-08-02 06:00:00.000000','2026-08-02 06:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000018','e4000000-0000-4000-8000-000000000017','e3000000-0000-4000-8000-000000000002','demo-return-0018','COMPLETED','2026-08-03 09:02:00.000000','2026-08-03 09:03:00.000000',NULL,'2026-08-03 09:00:00.000000','2026-08-03 09:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000020','e4000000-0000-4000-8000-000000000019','e3000000-0000-4000-8000-000000000003','demo-return-0020','COMPLETED','2026-08-04 03:02:00.000000','2026-08-04 03:03:00.000000',NULL,'2026-08-04 03:00:00.000000','2026-08-04 03:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000022','e4000000-0000-4000-8000-000000000021','e3000000-0000-4000-8000-000000000004','demo-return-0022','COMPLETED','2026-08-06 17:02:00.000000','2026-08-06 17:03:00.000000',NULL,'2026-08-06 17:00:00.000000','2026-08-06 17:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000024','e4000000-0000-4000-8000-000000000023','e3000000-0000-4000-8000-000000000005','demo-return-0024','COMPLETED','2026-08-06 09:02:00.000000','2026-08-06 09:03:00.000000',NULL,'2026-08-06 09:00:00.000000','2026-08-06 09:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000026','e4000000-0000-4000-8000-000000000025','e3000000-0000-4000-8000-000000000006','demo-return-0026','COMPLETED','2026-08-07 03:02:00.000000','2026-08-07 03:03:00.000000',NULL,'2026-08-07 03:00:00.000000','2026-08-07 03:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000028','e4000000-0000-4000-8000-000000000027','e3000000-0000-4000-8000-000000000007','demo-return-0028','COMPLETED','2026-07-26 06:02:00.000000','2026-07-26 06:03:00.000000',NULL,'2026-07-26 06:00:00.000000','2026-07-26 06:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000030','e4000000-0000-4000-8000-000000000029','e3000000-0000-4000-8000-000000000008','demo-return-0030','COMPLETED','2026-07-27 09:02:00.000000','2026-07-27 09:03:00.000000',NULL,'2026-07-27 09:00:00.000000','2026-07-27 09:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000032','e4000000-0000-4000-8000-000000000031','e3000000-0000-4000-8000-000000000009','demo-return-0032','COMPLETED','2026-07-28 03:02:00.000000','2026-07-28 03:03:00.000000',NULL,'2026-07-28 03:00:00.000000','2026-07-28 03:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000034','e4000000-0000-4000-8000-000000000033','e3000000-0000-4000-8000-000000000010','demo-return-0034','COMPLETED','2026-07-29 06:02:00.000000','2026-07-29 06:03:00.000000',NULL,'2026-07-29 06:00:00.000000','2026-07-29 06:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000036','e4000000-0000-4000-8000-000000000035','e3000000-0000-4000-8000-000000000001','demo-return-0036','COMPLETED','2026-07-31 20:02:00.000000','2026-07-31 20:03:00.000000',NULL,'2026-07-31 20:00:00.000000','2026-07-31 20:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000038','e4000000-0000-4000-8000-000000000037','e3000000-0000-4000-8000-000000000002','demo-return-0038','COMPLETED','2026-07-31 03:02:00.000000','2026-07-31 03:03:00.000000',NULL,'2026-07-31 03:00:00.000000','2026-07-31 03:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000040','e4000000-0000-4000-8000-000000000039','e3000000-0000-4000-8000-000000000003','demo-return-0040','COMPLETED','2026-08-01 06:02:00.000000','2026-08-01 06:03:00.000000',NULL,'2026-08-01 06:00:00.000000','2026-08-01 06:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000042','e4000000-0000-4000-8000-000000000041','e3000000-0000-4000-8000-000000000004','demo-return-0042','COMPLETED','2026-08-02 09:02:00.000000','2026-08-02 09:03:00.000000',NULL,'2026-08-02 09:00:00.000000','2026-08-02 09:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000044','e4000000-0000-4000-8000-000000000043','e3000000-0000-4000-8000-000000000005','demo-return-0044','COMPLETED','2026-08-03 03:02:00.000000','2026-08-03 03:03:00.000000',NULL,'2026-08-03 03:00:00.000000','2026-08-03 03:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000046','e4000000-0000-4000-8000-000000000045','e3000000-0000-4000-8000-000000000006','demo-return-0046','COMPLETED','2026-08-04 06:02:00.000000','2026-08-04 06:03:00.000000',NULL,'2026-08-04 06:00:00.000000','2026-08-04 06:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000048','e4000000-0000-4000-8000-000000000047','e3000000-0000-4000-8000-000000000007','demo-return-0048','COMPLETED','2026-08-05 09:02:00.000000','2026-08-05 09:03:00.000000',NULL,'2026-08-05 09:00:00.000000','2026-08-05 09:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000050','e4000000-0000-4000-8000-000000000049','e3000000-0000-4000-8000-000000000008','demo-return-0050','COMPLETED','2026-08-07 14:02:00.000000','2026-08-07 14:03:00.000000',NULL,'2026-08-07 14:00:00.000000','2026-08-07 14:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000052','e4000000-0000-4000-8000-000000000051','e3000000-0000-4000-8000-000000000009','demo-return-0052','COMPLETED','2026-08-07 06:02:00.000000','2026-08-07 06:03:00.000000',NULL,'2026-08-07 06:00:00.000000','2026-08-07 06:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000054','e4000000-0000-4000-8000-000000000053','e3000000-0000-4000-8000-000000000010','demo-return-0054','COMPLETED','2026-07-26 09:02:00.000000','2026-07-26 09:03:00.000000',NULL,'2026-07-26 09:00:00.000000','2026-07-26 09:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000056','e4000000-0000-4000-8000-000000000055','e3000000-0000-4000-8000-000000000001','demo-return-0056','COMPLETED','2026-07-27 03:02:00.000000','2026-07-27 03:03:00.000000',NULL,'2026-07-27 03:00:00.000000','2026-07-27 03:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000058','e4000000-0000-4000-8000-000000000057','e3000000-0000-4000-8000-000000000002','demo-return-0058','COMPLETED','2026-07-28 06:02:00.000000','2026-07-28 06:03:00.000000',NULL,'2026-07-28 06:00:00.000000','2026-07-28 06:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000060','e4000000-0000-4000-8000-000000000059','e3000000-0000-4000-8000-000000000003','demo-return-0060','COMPLETED','2026-07-29 09:02:00.000000','2026-07-29 09:03:00.000000',NULL,'2026-07-29 09:00:00.000000','2026-07-29 09:03:00.000000');
INSERT INTO `return_attempt` (`return_attempt_id`, `rental_id`, `return_slot_id`, `request_id`, `status`, `physical_completed_at`, `completed_at`, `failure_reason`, `created_at`, `updated_at`) VALUES ('e5000000-0000-4000-8000-000000000096','e4000000-0000-4000-8000-000000000095',NULL,'demo-return-0096','PROCESSING',NULL,NULL,NULL,'2026-08-08 21:00:00.000000','2026-08-08 21:00:00.000000');
/*!40000 ALTER TABLE `return_attempt` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `TRG_RETURN_ATTEMPT_BEFORE_INSERT` BEFORE INSERT ON `return_attempt` FOR EACH ROW BEGIN
	DECLARE `rental_status_value` VARCHAR(30) DEFAULT NULL;
	DECLARE CONTINUE HANDLER FOR NOT FOUND
		SET `rental_status_value` = NULL;

	IF `NEW`.`status` <> 'FAILED' THEN
		SELECT `status`
		INTO `rental_status_value`
		FROM `rental`
		WHERE `rental_id` = `NEW`.`rental_id`
		FOR UPDATE;

		IF `rental_status_value` <> 'RETURNING'
			OR `rental_status_value` IS NULL THEN
			SIGNAL SQLSTATE '45000'
				SET MESSAGE_TEXT = 'RETURN_RENTAL_NOT_RETURNING';
		END IF;
	END IF;

	IF `NEW`.`status` = 'COMPLETED' THEN
		UPDATE `rental`
		SET
			`status` = 'COMPLETED',
			`ended_at` = `NEW`.`completed_at`
		WHERE `rental_id` = `NEW`.`rental_id`
			AND `status` = 'RETURNING';

		IF ROW_COUNT() <> 1 THEN
			SIGNAL SQLSTATE '45000'
				SET MESSAGE_TEXT = 'RETURN_RENTAL_COMPLETION_CONFLICT';
		END IF;
	END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `TRG_RETURN_ATTEMPT_BEFORE_UPDATE` BEFORE UPDATE ON `return_attempt` FOR EACH ROW BEGIN
	DECLARE `rental_status_value` VARCHAR(30) DEFAULT NULL;
	DECLARE CONTINUE HANDLER FOR NOT FOUND
		SET `rental_status_value` = NULL;

	IF `NEW`.`rental_id` <> `OLD`.`rental_id` THEN
		SIGNAL SQLSTATE '45000'
			SET MESSAGE_TEXT = 'RETURN_RENTAL_IMMUTABLE';
	END IF;

	IF `OLD`.`status` = 'COMPLETED'
		AND (
			`NEW`.`status` <> 'COMPLETED'
			OR NOT (`NEW`.`completed_at` <=> `OLD`.`completed_at`)
			OR NOT (
				`NEW`.`physical_completed_at`
					<=> `OLD`.`physical_completed_at`
			)
			OR NOT (`NEW`.`return_slot_id` <=> `OLD`.`return_slot_id`)
		) THEN
		SIGNAL SQLSTATE '45000'
			SET MESSAGE_TEXT = 'COMPLETED_RETURN_IS_TERMINAL';
	END IF;

	IF `OLD`.`status` <> 'COMPLETED'
		AND `NEW`.`status` <> 'FAILED' THEN
		SELECT `status`
		INTO `rental_status_value`
		FROM `rental`
		WHERE `rental_id` = `NEW`.`rental_id`
		FOR UPDATE;

		IF `rental_status_value` <> 'RETURNING'
			OR `rental_status_value` IS NULL THEN
			SIGNAL SQLSTATE '45000'
				SET MESSAGE_TEXT = 'RETURN_RENTAL_NOT_RETURNING';
		END IF;
	END IF;

	IF `OLD`.`status` <> 'COMPLETED'
		AND `NEW`.`status` = 'COMPLETED' THEN
		UPDATE `rental`
		SET
			`status` = 'COMPLETED',
			`ended_at` = `NEW`.`completed_at`
		WHERE `rental_id` = `NEW`.`rental_id`
			AND `status` = 'RETURNING';

		IF ROW_COUNT() <> 1 THEN
			SIGNAL SQLSTATE '45000'
				SET MESSAGE_TEXT = 'RETURN_RENTAL_COMPLETION_CONFLICT';
		END IF;
	END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `settlement`
--

DROP TABLE IF EXISTS `settlement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `settlement` (
  `settlement_id` char(36) NOT NULL,
  `user_id` char(9) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `rental_id` char(36) NOT NULL,
  `damage_inspection_id` char(36) DEFAULT NULL COMMENT 'Administrator-reviewed DAMAGE_INSPECTION used as DAMAGE settlement basis',
  `reason` varchar(50) NOT NULL,
  `amount` bigint unsigned NOT NULL,
  `paid_amount` bigint unsigned NOT NULL DEFAULT '0',
  `status` varchar(30) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `paid_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `damage_basis_guard` tinyint GENERATED ALWAYS AS ((case when ((`reason` = _utf8mb4'DAMAGE') and (`damage_inspection_id` is not null)) then 1 else NULL end)) STORED,
  PRIMARY KEY (`settlement_id`),
  UNIQUE KEY `UK_SETTLEMENT_RENTAL_ID` (`rental_id`),
  UNIQUE KEY `UK_SETTLEMENT_DAMAGE_INSPECTION_ID` (`damage_inspection_id`),
  KEY `FK_SETTLEMENT_RENTAL_USER` (`rental_id`,`user_id`),
  KEY `FK_SETTLEMENT_DAMAGE_INSPECTION` (`damage_inspection_id`,`rental_id`,`damage_basis_guard`),
  CONSTRAINT `FK_SETTLEMENT_DAMAGE_INSPECTION` FOREIGN KEY (`damage_inspection_id`, `rental_id`, `damage_basis_guard`) REFERENCES `damage_inspection` (`inspection_id`, `rental_id`, `settlement_eligible_guard`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FK_SETTLEMENT_RENTAL_USER` FOREIGN KEY (`rental_id`, `user_id`) REFERENCES `rental` (`rental_id`, `user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `CK_SETTLEMENT_AMOUNT` CHECK ((`amount` >= 0)),
  CONSTRAINT `CK_SETTLEMENT_AMOUNT_CAP` CHECK ((`amount` <= 7000)),
  CONSTRAINT `CK_SETTLEMENT_AUDIT_TIME_ORDER` CHECK ((`updated_at` >= `created_at`)),
  CONSTRAINT `CK_SETTLEMENT_DAMAGE_BASIS` CHECK ((((`reason` = _utf8mb4'DAMAGE') and (`damage_inspection_id` is not null)) or ((`reason` <> _utf8mb4'DAMAGE') and (`damage_inspection_id` is null)))),
  CONSTRAINT `CK_SETTLEMENT_ENUM_STORAGE` CHECK ((regexp_like(`reason`,_utf8mb4'^[A-Z][A-Z0-9_]{0,49}$',_utf8mb4'c') and regexp_like(`status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c'))),
  CONSTRAINT `CK_SETTLEMENT_PAID_AMOUNT` CHECK (((`paid_amount` >= 0) and (`paid_amount` <= `amount`))),
  CONSTRAINT `CK_SETTLEMENT_PAID_TIME_ORDER` CHECK (((`paid_at` is null) or (`paid_at` >= `created_at`))),
  CONSTRAINT `CK_SETTLEMENT_PAYMENT_STATE` CHECK ((((`status` = _utf8mb4'PAID') and (`paid_amount` = `amount`) and (`paid_at` is not null)) or ((`status` = _utf8mb4'PENDING') and (`paid_amount` < `amount`) and (`paid_at` is null)) or ((`status` = _utf8mb4'CANCELLED') and (`paid_amount` = 0) and (`paid_at` is null)))),
  CONSTRAINT `CK_SETTLEMENT_REASON` CHECK ((`reason` in (_utf8mb4'OVERDUE',_utf8mb4'LOSS',_utf8mb4'DAMAGE',_utf8mb4'ADJUSTMENT'))),
  CONSTRAINT `CK_SETTLEMENT_REASON_AMOUNT` CHECK ((((`reason` in (_utf8mb4'DAMAGE',_utf8mb4'LOSS')) and (`amount` = 7000)) or ((`reason` = _utf8mb4'OVERDUE') and (`amount` between 1000 and 6000) and ((`amount` % 1000) = 0)) or ((`reason` = _utf8mb4'ADJUSTMENT') and (`amount` between 0 and 7000)))),
  CONSTRAINT `CK_SETTLEMENT_STATUS` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'PAID',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `settlement`
--

LOCK TABLES `settlement` WRITE;
/*!40000 ALTER TABLE `settlement` DISABLE KEYS */;
INSERT INTO `settlement` (`settlement_id`, `user_id`, `rental_id`, `damage_inspection_id`, `reason`, `amount`, `paid_amount`, `status`, `created_at`, `paid_at`, `updated_at`) VALUES ('77000000-0000-4000-8000-000000000004','202600001','44000000-0000-4000-8000-000000000004',NULL,'ADJUSTMENT',4000,4000,'PAID','2026-08-03 05:05:00.000000','2026-08-03 05:16:00.000000','2026-08-03 05:20:00.000000');
INSERT INTO `settlement` (`settlement_id`, `user_id`, `rental_id`, `damage_inspection_id`, `reason`, `amount`, `paid_amount`, `status`, `created_at`, `paid_at`, `updated_at`) VALUES ('77000000-0000-4000-8000-000000000005','202600005','44000000-0000-4000-8000-000000000003','66000000-0000-4000-8000-000000000001','DAMAGE',7000,7000,'PAID','2026-08-03 05:00:00.000000','2026-08-03 05:11:00.000000','2026-08-03 05:12:00.000000');
INSERT INTO `settlement` (`settlement_id`, `user_id`, `rental_id`, `damage_inspection_id`, `reason`, `amount`, `paid_amount`, `status`, `created_at`, `paid_at`, `updated_at`) VALUES ('77000000-0000-4000-8000-000000000006','202600003','44000000-0000-4000-8000-000000000009',NULL,'OVERDUE',1000,1000,'PAID','2026-08-03 05:02:00.000000','2026-08-03 05:13:00.000000','2026-08-03 05:14:00.000000');
INSERT INTO `settlement` (`settlement_id`, `user_id`, `rental_id`, `damage_inspection_id`, `reason`, `amount`, `paid_amount`, `status`, `created_at`, `paid_at`, `updated_at`) VALUES ('77000000-0000-4000-8000-000000000007','202600002','44000000-0000-4000-8000-000000000007',NULL,'OVERDUE',2000,2000,'PAID','2026-08-03 05:00:00.000000','2026-08-03 05:23:00.000000','2026-08-03 05:24:00.000000');
INSERT INTO `settlement` (`settlement_id`, `user_id`, `rental_id`, `damage_inspection_id`, `reason`, `amount`, `paid_amount`, `status`, `created_at`, `paid_at`, `updated_at`) VALUES ('e7000000-0000-4000-8000-000000000080','202512004','e4000000-0000-4000-8000-000000000007',NULL,'OVERDUE',2000,2000,'PAID','2026-07-30 14:07:00.000000','2026-07-30 14:17:20.000000','2026-08-08 14:19:54.944153');
INSERT INTO `settlement` (`settlement_id`, `user_id`, `rental_id`, `damage_inspection_id`, `reason`, `amount`, `paid_amount`, `status`, `created_at`, `paid_at`, `updated_at`) VALUES ('e7000000-0000-4000-8000-000000000081','202512011','e4000000-0000-4000-8000-000000000021',NULL,'OVERDUE',2000,2000,'PAID','2026-08-06 17:07:00.000000','2026-08-06 17:17:20.000000','2026-08-08 14:19:54.944153');
INSERT INTO `settlement` (`settlement_id`, `user_id`, `rental_id`, `damage_inspection_id`, `reason`, `amount`, `paid_amount`, `status`, `created_at`, `paid_at`, `updated_at`) VALUES ('e7000000-0000-4000-8000-000000000082','202512001','e4000000-0000-4000-8000-000000000025','e6000000-0000-4000-8000-000000000073','DAMAGE',7000,7000,'PAID','2026-08-07 04:04:09.000000','2026-08-07 04:14:29.000000','2026-08-08 14:19:54.944153');
INSERT INTO `settlement` (`settlement_id`, `user_id`, `rental_id`, `damage_inspection_id`, `reason`, `amount`, `paid_amount`, `status`, `created_at`, `paid_at`, `updated_at`) VALUES ('e7000000-0000-4000-8000-000000000083','202512002','e4000000-0000-4000-8000-000000000027','e6000000-0000-4000-8000-000000000074','DAMAGE',7000,7000,'PAID','2026-07-26 07:04:09.000000','2026-07-26 07:14:29.000000','2026-08-08 14:19:54.944153');
INSERT INTO `settlement` (`settlement_id`, `user_id`, `rental_id`, `damage_inspection_id`, `reason`, `amount`, `paid_amount`, `status`, `created_at`, `paid_at`, `updated_at`) VALUES ('e7000000-0000-4000-8000-000000000084','202512003','e4000000-0000-4000-8000-000000000029','e6000000-0000-4000-8000-000000000075','DAMAGE',7000,7000,'PAID','2026-07-27 10:04:09.000000','2026-07-27 10:14:29.000000','2026-08-08 14:19:54.944153');
INSERT INTO `settlement` (`settlement_id`, `user_id`, `rental_id`, `damage_inspection_id`, `reason`, `amount`, `paid_amount`, `status`, `created_at`, `paid_at`, `updated_at`) VALUES ('e7000000-0000-4000-8000-000000000085','202512006','e4000000-0000-4000-8000-000000000035',NULL,'OVERDUE',2000,0,'PENDING','2026-07-31 20:07:00.000000',NULL,'2026-07-31 20:07:00.000000');
INSERT INTO `settlement` (`settlement_id`, `user_id`, `rental_id`, `damage_inspection_id`, `reason`, `amount`, `paid_amount`, `status`, `created_at`, `paid_at`, `updated_at`) VALUES ('e7000000-0000-4000-8000-000000000086','202512001','e4000000-0000-4000-8000-000000000049',NULL,'OVERDUE',2000,0,'PENDING','2026-08-07 14:07:00.000000',NULL,'2026-08-07 14:07:00.000000');
/*!40000 ALTER TABLE `settlement` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `TRG_SETTLEMENT_BEFORE_INSERT` BEFORE INSERT ON `settlement` FOR EACH ROW BEGIN
	IF `NEW`.`paid_amount` <> 0
		OR `NEW`.`paid_at` IS NOT NULL
		OR `NEW`.`status` = 'PAID' THEN
		SIGNAL SQLSTATE '45000'
			SET MESSAGE_TEXT = 'SETTLEMENT_MUST_START_WITHOUT_CREDIT';
	END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `TRG_SETTLEMENT_PAYMENT_BEFORE_UPDATE` BEFORE UPDATE ON `settlement` FOR EACH ROW BEGIN
	DECLARE `guard_consumed` INT DEFAULT 0;
	DECLARE `allowed_unpaid_cancel` BOOLEAN DEFAULT FALSE;
	DECLARE `allowed_upward_reprice` BOOLEAN DEFAULT FALSE;

	IF `OLD`.`damage_inspection_id` IS NOT NULL
		AND (
			`NEW`.`reason` <> `OLD`.`reason`
			OR NOT (
				`NEW`.`damage_inspection_id`
					<=> `OLD`.`damage_inspection_id`
			)
		) THEN
		SIGNAL SQLSTATE '45000'
			SET MESSAGE_TEXT = 'DAMAGE_SETTLEMENT_BASIS_IMMUTABLE';
	END IF;

	SET `allowed_unpaid_cancel` = (
		`OLD`.`status` = 'PENDING'
		AND `NEW`.`status` = 'CANCELLED'
		AND `OLD`.`paid_amount` = 0
		AND `NEW`.`paid_amount` = 0
		AND `OLD`.`paid_at` IS NULL
		AND `NEW`.`paid_at` IS NULL
	);

	SET `allowed_upward_reprice` = (
		`OLD`.`status` = 'PAID'
		AND `NEW`.`status` = 'PENDING'
		AND `OLD`.`paid_amount` = `NEW`.`paid_amount`
		AND `NEW`.`amount` > `OLD`.`amount`
		AND `NEW`.`paid_amount` < `NEW`.`amount`
		AND `OLD`.`paid_at` IS NOT NULL
		AND `NEW`.`paid_at` IS NULL
	);

	IF `NEW`.`paid_amount` <> `OLD`.`paid_amount` THEN
		DELETE FROM `settlement_payment_mutation_guard`
		WHERE `connection_id` = CONNECTION_ID()
			AND `settlement_id` = `OLD`.`settlement_id`
			AND `expected_paid_amount` = `NEW`.`paid_amount`
			AND `expected_status` = `NEW`.`status`
			AND `expected_paid_at` <=> `NEW`.`paid_at`;

		SET `guard_consumed` = ROW_COUNT();

		IF `guard_consumed` <> 1 THEN
			SIGNAL SQLSTATE '45000'
				SET MESSAGE_TEXT = 'SETTLEMENT_PAYMENT_FIELDS_MANAGED';
		END IF;
	ELSEIF `NEW`.`status` <> `OLD`.`status`
		OR NOT (`NEW`.`paid_at` <=> `OLD`.`paid_at`) THEN
		IF NOT `allowed_unpaid_cancel`
			AND NOT `allowed_upward_reprice` THEN
			SIGNAL SQLSTATE '45000'
				SET MESSAGE_TEXT = 'SETTLEMENT_PAYMENT_FIELDS_MANAGED';
		END IF;
	END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `TRG_SETTLEMENT_DAMAGE_BASIS_BEFORE_DELETE` BEFORE DELETE ON `settlement` FOR EACH ROW BEGIN
	IF `OLD`.`damage_inspection_id` IS NOT NULL THEN
		SIGNAL SQLSTATE '45000'
			SET MESSAGE_TEXT = 'DAMAGE_SETTLEMENT_DELETE_FORBIDDEN';
	END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `settlement_payment_mutation_guard`
--

DROP TABLE IF EXISTS `settlement_payment_mutation_guard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `settlement_payment_mutation_guard` (
  `connection_id` bigint unsigned NOT NULL,
  `settlement_id` char(36) NOT NULL,
  `expected_paid_amount` bigint unsigned NOT NULL,
  `expected_status` varchar(30) NOT NULL,
  `expected_paid_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`connection_id`),
  CONSTRAINT `CK_SETTLEMENT_PAYMENT_MUTATION_GUARD_STATUS` CHECK ((`expected_status` in (_utf8mb4'PENDING',_utf8mb4'PAID')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Internal one-use trigger handshake; no application DML grants';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `settlement_payment_mutation_guard`
--

LOCK TABLES `settlement_payment_mutation_guard` WRITE;
/*!40000 ALTER TABLE `settlement_payment_mutation_guard` DISABLE KEYS */;
/*!40000 ALTER TABLE `settlement_payment_mutation_guard` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `slot`
--

DROP TABLE IF EXISTS `slot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `slot` (
  `slot_id` char(36) NOT NULL,
  `station_id` char(36) NOT NULL,
  `slot_number` int NOT NULL,
  `service_status` varchar(30) NOT NULL,
  `occupancy_status` varchar(30) NOT NULL,
  `lock_status` varchar(30) NOT NULL,
  `item_condition` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`slot_id`),
  UNIQUE KEY `UK_SLOT_STATION_NUMBER` (`station_id`,`slot_number`),
  UNIQUE KEY `UK_SLOT_ID_STATION_ID` (`slot_id`,`station_id`),
  CONSTRAINT `FK_SLOT_STATION` FOREIGN KEY (`station_id`) REFERENCES `station` (`station_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `CK_SLOT_AUDIT_TIME_ORDER` CHECK ((`updated_at` >= `created_at`)),
  CONSTRAINT `CK_SLOT_ENUM_STORAGE` CHECK ((regexp_like(`service_status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c') and regexp_like(`occupancy_status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c') and regexp_like(`lock_status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c') and ((`item_condition` is null) or regexp_like(`item_condition`,_utf8mb4'^[A-Z][A-Z0-9_]{0,49}$',_utf8mb4'c'))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `slot`
--

LOCK TABLES `slot` WRITE;
/*!40000 ALTER TABLE `slot` DISABLE KEYS */;
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('33000000-0000-4000-8000-000000000001','22000000-0000-4000-8000-000000000001',1,'AVAILABLE','OCCUPIED','LOCKED','NORMAL','2026-08-03 00:00:00.000000','2026-08-03 05:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('33000000-0000-4000-8000-000000000002','22000000-0000-4000-8000-000000000001',2,'ADMIN_REVIEW','OCCUPIED','LOCKED','UNKNOWN','2026-08-03 00:00:00.000000','2026-08-03 05:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('33000000-0000-4000-8000-000000000003','22000000-0000-4000-8000-000000000001',3,'ADMIN_REVIEW','OCCUPIED','LOCKED','UNKNOWN','2026-08-03 00:00:00.000000','2026-08-03 05:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('33000000-0000-4000-8000-000000000004','22000000-0000-4000-8000-000000000001',4,'AVAILABLE','OCCUPIED','LOCKED','NORMAL','2026-08-03 00:00:00.000000','2026-08-03 05:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('33000000-0000-4000-8000-000000000005','22000000-0000-4000-8000-000000000001',5,'OUT_OF_SERVICE','OCCUPIED','LOCKED','DAMAGED','2026-08-03 00:00:00.000000','2026-08-03 05:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('e3000000-0000-4000-8000-000000000001','e1000000-0000-4000-8000-000000000002',1,'AVAILABLE','OCCUPIED','LOCKED','NORMAL','2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('e3000000-0000-4000-8000-000000000002','e1000000-0000-4000-8000-000000000002',2,'AVAILABLE','OCCUPIED','LOCKED','NORMAL','2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('e3000000-0000-4000-8000-000000000003','e1000000-0000-4000-8000-000000000002',3,'AVAILABLE','EMPTY','LOCKED','EMPTY','2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('e3000000-0000-4000-8000-000000000004','e1000000-0000-4000-8000-000000000002',4,'AVAILABLE','OCCUPIED','LOCKED','NORMAL','2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('e3000000-0000-4000-8000-000000000005','e1000000-0000-4000-8000-000000000002',5,'OUT_OF_SERVICE','OCCUPIED','LOCKED','DAMAGED','2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('e3000000-0000-4000-8000-000000000006','e1000000-0000-4000-8000-000000000003',1,'AVAILABLE','OCCUPIED','LOCKED','NORMAL','2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('e3000000-0000-4000-8000-000000000007','e1000000-0000-4000-8000-000000000003',2,'AVAILABLE','EMPTY','LOCKED','EMPTY','2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('e3000000-0000-4000-8000-000000000008','e1000000-0000-4000-8000-000000000003',3,'AVAILABLE','OCCUPIED','LOCKED','NORMAL','2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('e3000000-0000-4000-8000-000000000009','e1000000-0000-4000-8000-000000000003',4,'ADMIN_REVIEW','OCCUPIED','ERROR','UNKNOWN','2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000');
INSERT INTO `slot` (`slot_id`, `station_id`, `slot_number`, `service_status`, `occupancy_status`, `lock_status`, `item_condition`, `created_at`, `updated_at`) VALUES ('e3000000-0000-4000-8000-000000000010','e1000000-0000-4000-8000-000000000003',5,'AVAILABLE','OCCUPIED','LOCKED','NORMAL','2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000');
/*!40000 ALTER TABLE `slot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `station`
--

DROP TABLE IF EXISTS `station`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `station` (
  `station_id` char(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `service_status` varchar(30) NOT NULL,
  `device_status` varchar(30) NOT NULL,
  `current_boot_id` varchar(100) DEFAULT NULL,
  `boot_synced_at` datetime(6) DEFAULT NULL,
  `last_seen_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`station_id`),
  CONSTRAINT `CK_STATION_AUDIT_TIME_ORDER` CHECK ((`updated_at` >= `created_at`)),
  CONSTRAINT `CK_STATION_ENUM_STORAGE` CHECK ((regexp_like(`service_status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c') and regexp_like(`device_status`,_utf8mb4'^[A-Z][A-Z0-9_]{0,29}$',_utf8mb4'c')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `station`
--

LOCK TABLES `station` WRITE;
/*!40000 ALTER TABLE `station` DISABLE KEYS */;
INSERT INTO `station` (`station_id`, `name`, `service_status`, `device_status`, `current_boot_id`, `boot_synced_at`, `last_seen_at`, `created_at`, `updated_at`) VALUES ('22000000-0000-4000-8000-000000000001','Fixture Station 01','AVAILABLE','ONLINE','fixture-boot-station-01','2026-08-03 00:00:00.000000','2026-08-03 00:00:00.000000','2026-08-03 00:00:00.000000','2026-08-03 00:00:00.000000');
INSERT INTO `station` (`station_id`, `name`, `service_status`, `device_status`, `current_boot_id`, `boot_synced_at`, `last_seen_at`, `created_at`, `updated_at`) VALUES ('e1000000-0000-4000-8000-000000000002','멀티캠퍼스 역삼 1층 로비','AVAILABLE','ONLINE','demo-boot-station-02','2026-07-25 09:00:00.000000','2026-08-08 08:00:00.000000','2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000');
INSERT INTO `station` (`station_id`, `name`, `service_status`, `device_status`, `current_boot_id`, `boot_synced_at`, `last_seen_at`, `created_at`, `updated_at`) VALUES ('e1000000-0000-4000-8000-000000000003','멀티캠퍼스 역삼 8층 라운지','AVAILABLE','ONLINE','demo-boot-station-03','2026-07-25 09:00:00.000000','2026-08-08 08:00:00.000000','2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000');
/*!40000 ALTER TABLE `station` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_account`
--

DROP TABLE IF EXISTS `user_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_account` (
  `user_id` char(9) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Exact nine-digit student number; Spring/MySQL business boundary only',
  `user_ref` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `login_id` varchar(254) NOT NULL COMMENT 'Normalized university email; never the student number',
  `password_hash` varchar(255) NOT NULL,
  `name` varchar(100) NOT NULL,
  `face_registered` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `rental_eligible` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Fail-closed rental eligibility projection; not a business SSOT',
  `rental_eligibility_evaluated_at` datetime(6) DEFAULT NULL COMMENT 'Last authoritative evaluation time; NULL means not evaluated yet',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UK_USER_ACCOUNT_LOGIN_ID` (`login_id`),
  UNIQUE KEY `UK_USER_ACCOUNT_USER_REF` (`user_ref`),
  CONSTRAINT `CK_USER_ACCOUNT_AUDIT_TIME_ORDER` CHECK ((`updated_at` >= `created_at`)),
  CONSTRAINT `CK_USER_ACCOUNT_LOGIN_ID_EMAIL` CHECK (((char_length(`login_id`) between 3 and 254) and (cast(`login_id` as char charset binary) = cast(lower(trim(`login_id`)) as char charset binary)) and regexp_like(`login_id`,_utf8mb4'^[^[:space:]@]+@[^[:space:]@]+[.][^[:space:]@]+$',_utf8mb4'c'))),
  CONSTRAINT `CK_USER_ACCOUNT_RENTAL_ELIGIBILITY_EVALUATED` CHECK (((`rental_eligible` = false) or (`rental_eligibility_evaluated_at` is not null))),
  CONSTRAINT `CK_USER_ACCOUNT_RENTAL_ELIGIBLE` CHECK ((`rental_eligible` in (0,1))),
  CONSTRAINT `CK_USER_ACCOUNT_STUDENT_NUMBER` CHECK (regexp_like(`user_id`,_ascii'^[0-9]{9}$',_utf8mb4'c')),
  CONSTRAINT `CK_USER_ACCOUNT_USER_REF_UUID` CHECK (regexp_like(`user_ref`,_ascii'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',_utf8mb4'c'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_account`
--

LOCK TABLES `user_account` WRITE;
/*!40000 ALTER TABLE `user_account` DISABLE KEYS */;
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202512001','e0000000-0000-4000-8000-000000000001','demo01@billisan.demo','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','데모사용자01',1,'2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000',0,'2026-08-07 23:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202512002','e0000000-0000-4000-8000-000000000002','demo02@billisan.demo','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','데모사용자02',1,'2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000',1,'2026-08-07 23:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202512003','e0000000-0000-4000-8000-000000000003','demo03@billisan.demo','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','데모사용자03',1,'2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000',1,'2026-08-07 23:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202512004','e0000000-0000-4000-8000-000000000004','demo04@billisan.demo','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','데모사용자04',1,'2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000',0,'2026-08-07 23:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202512005','e0000000-0000-4000-8000-000000000005','demo05@billisan.demo','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','데모사용자05',1,'2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000',1,'2026-08-07 23:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202512006','e0000000-0000-4000-8000-000000000006','demo06@billisan.demo','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','데모사용자06',1,'2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000',0,'2026-08-07 23:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202512007','e0000000-0000-4000-8000-000000000007','demo07@billisan.demo','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','데모사용자07',1,'2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000',0,'2026-08-07 23:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202512008','e0000000-0000-4000-8000-000000000008','demo08@billisan.demo','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','데모사용자08',1,'2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000',0,'2026-08-07 23:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202512009','e0000000-0000-4000-8000-000000000009','demo09@billisan.demo','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','데모사용자09',1,'2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000',0,'2026-08-07 23:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202512010','e0000000-0000-4000-8000-000000000010','demo10@billisan.demo','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','데모사용자10',1,'2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000',1,'2026-08-07 23:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202512011','e0000000-0000-4000-8000-000000000011','demo11@billisan.demo','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','데모사용자11',0,'2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000',0,'2026-08-07 23:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202512012','e0000000-0000-4000-8000-000000000012','demo12@billisan.demo','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','데모사용자12',0,'2026-07-25 09:00:00.000000','2026-07-25 09:00:00.000000',0,'2026-08-07 23:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202600001','f6f509d4-c001-446c-b6b7-537d65a093c3','demo01.fixture@billisan.test','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','제갈한동',1,'2026-08-01 00:00:00.000000','2026-08-03 00:00:00.000000',1,'2026-08-03 00:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202600002','f86a8791-c02b-4e50-b761-d9dd009909a9','demo02.fixture@billisan.test','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','장혜진',1,'2026-08-01 00:00:00.000000','2026-08-03 00:00:00.000000',1,'2026-08-03 00:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202600003','0b8255a1-70e4-45af-80f2-b38e4164ea06','demo03.fixture@billisan.test','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','곽도연',1,'2026-08-01 00:00:00.000000','2026-08-03 00:00:00.000000',1,'2026-08-03 00:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202600004','59b97d2b-a37d-4f75-bd33-4beb1bd3ac76','demo04.fixture@billisan.test','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','이다인',1,'2026-08-01 00:00:00.000000','2026-08-03 00:00:00.000000',1,'2026-08-03 00:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202600005','d8a6f8d6-3c43-4789-b980-a9f6918d53b8','demo05.fixture@billisan.test','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','이원준',1,'2026-08-01 00:00:00.000000','2026-08-03 00:00:00.000000',1,'2026-08-03 00:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202600006','3650e75c-ab2b-4455-8f00-c691fd2e66fb','demo06.fixture@billisan.test','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','이경호',1,'2026-08-01 00:00:00.000000','2026-08-03 00:00:00.000000',1,'2026-08-03 00:00:00.000000');
INSERT INTO `user_account` (`user_id`, `user_ref`, `login_id`, `password_hash`, `name`, `face_registered`, `created_at`, `updated_at`, `rental_eligible`, `rental_eligibility_evaluated_at`) VALUES ('202600007','bb14cac2-74d1-4b7f-a69f-aa88fcf8f55e','face-unregistered.fixture@billisan.test','$2a$10$WS6.9ytXBl7MzDCnzrKuFuLrp/xOB4BM.HRGDWCeFzNrLvjRgCF0u','얼굴 미등록 사용자',0,'2026-08-01 00:00:00.000000','2026-08-03 00:00:00.000000',0,'2026-08-03 00:00:00.000000');
/*!40000 ALTER TABLE `user_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'billisan'
--

--
-- Dumping routines for database 'billisan'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-08  5:20:40
