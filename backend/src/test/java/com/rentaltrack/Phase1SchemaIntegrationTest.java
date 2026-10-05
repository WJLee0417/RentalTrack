package com.rentaltrack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class Phase1SchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long memberId;
    private Long locationId;
    private Long categoryId;
    private Long productId;
    private Long contractId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        jdbcTemplate.update(
                "INSERT INTO member_account (email, name, status) VALUES (?, ?, 'ACTIVE')",
                "phase1-" + UUID.randomUUID() + "@example.com",
                "Phase1 Tester"
        );
        memberId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM member_account", Long.class);

        String locationCode = "LOC-" + shortId();
        jdbcTemplate.update(
                """
                INSERT INTO rental_location
                    (code, name, address, latitude, longitude, active)
                VALUES (?, 'Phase1 Location', 'Seoul', 37.5000000, 127.0000000, TRUE)
                """,
                locationCode
        );
        locationId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM rental_location", Long.class);

        String categoryCode = "CAT-" + shortId();
        jdbcTemplate.update(
                """
                INSERT INTO product_category (code, name, description, active)
                VALUES (?, 'Portable Item', 'Phase1 validation category', TRUE)
                """,
                categoryCode
        );
        categoryId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM product_category", Long.class);

        String assetCode = "ASSET-" + shortId();
        jdbcTemplate.update(
                """
                INSERT INTO rental_product
                    (asset_code, display_name, category_id, current_location_id, status, version)
                VALUES (?, 'Phase1 Product', ?, ?, 'AVAILABLE', 0)
                """,
                assetCode,
                categoryId,
                locationId
        );
        productId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM rental_product", Long.class);

        String contractCode = "CONTRACT-" + shortId();
        jdbcTemplate.update(
                """
                INSERT INTO rental_contract
                    (contract_code, member_id, rental_product_id, pickup_location_id,
                     status, requested_at, version)
                VALUES (?, ?, ?, ?, 'REQUESTED', ?, 0)
                """,
                contractCode,
                memberId,
                productId,
                locationId,
                LocalDateTime.now()
        );
        contractId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM rental_contract", Long.class);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void flywayV1ToV5AreAppliedAndJpaSchemaValidationSucceeds() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version IN ('1','2','3','4','5')",
                Integer.class
        );

        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                """,
                String.class
        );

        assertThat(migrationCount).isEqualTo(5);
        assertThat(tables).contains(
                "member_account",
                "rental_location",
                "product_category",
                "rental_product",
                "rental_contract",
                "condition_record",
                "condition_image",
                "damage_record",
                "return_inspection"
        );
    }

    @Test
    void conditionRecordAllowsOnlyOneRecordPerContractAndType() {
        insertConditionRecord("BEFORE_RENTAL");

        assertThatThrownBy(() -> insertConditionRecord("BEFORE_RENTAL"))
                .isInstanceOf(DataIntegrityViolationException.class);

        insertConditionRecord("AFTER_RENTAL");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM condition_record WHERE rental_contract_id = ?",
                Integer.class,
                contractId
        );
        assertThat(count).isEqualTo(2);
    }

    @Test
    void damageRecordTracksExistingDamageWithoutDuplicateMatching() {
        Long beforeConditionId = insertConditionRecord("BEFORE_RENTAL");
        Long afterConditionId = insertConditionRecord("AFTER_RENTAL");

        jdbcTemplate.update(
                """
                INSERT INTO damage_record
                    (condition_record_id, damage_type, damage_location, severity,
                     description, comparison_result)
                VALUES (?, 'SCRATCH', 'LEFT_SIDE', 'MINOR', 'baseline scratch', 'BASELINE')
                """,
                beforeConditionId
        );
        Long beforeDamageId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM damage_record",
                Long.class
        );

        jdbcTemplate.update(
                """
                INSERT INTO damage_record
                    (condition_record_id, damage_type, damage_location, severity,
                     description, comparison_result, matched_before_damage_id)
                VALUES (?, 'SCRATCH', 'LEFT_SIDE', 'MINOR', 'same scratch', 'EXISTING', ?)
                """,
                afterConditionId,
                beforeDamageId
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO damage_record
                    (condition_record_id, damage_type, damage_location, severity,
                     description, comparison_result, matched_before_damage_id)
                VALUES (?, 'SCRATCH', 'LEFT_SIDE', 'MINOR', 'duplicate match', 'EXISTING', ?)
                """,
                afterConditionId,
                beforeDamageId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void returnInspectionAllowsOnlyOneFinalInspectionPerContract() {
        jdbcTemplate.update(
                """
                INSERT INTO return_inspection
                    (rental_contract_id, result, note, inspected_at, version)
                VALUES (?, 'NO_NEW_DAMAGE', 'first inspection', ?, 0)
                """,
                contractId,
                LocalDateTime.now()
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO return_inspection
                    (rental_contract_id, result, note, inspected_at, version)
                VALUES (?, 'REVIEW_REQUIRED', 'duplicate inspection', ?, 0)
                """,
                contractId,
                LocalDateTime.now()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsInvalidProductAndContractStateValues() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE rental_product SET status = 'BROKEN_STATE' WHERE id = ?",
                productId
        )).isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE rental_contract SET status = 'BROKEN_STATE' WHERE id = ?",
                contractId
        )).isInstanceOf(DataAccessException.class);
    }

    private Long insertConditionRecord(String recordType) {
        jdbcTemplate.update(
                """
                INSERT INTO condition_record
                    (rental_contract_id, record_type, recorded_by_member_id, summary, recorded_at)
                VALUES (?, ?, ?, 'phase1 validation', ?)
                """,
                contractId,
                recordType,
                memberId,
                LocalDateTime.now()
        );

        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM condition_record
                WHERE rental_contract_id = ? AND record_type = ?
                """,
                Long.class,
                contractId,
                recordType
        );
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE return_inspection");
        jdbcTemplate.execute("TRUNCATE TABLE damage_record");
        jdbcTemplate.execute("TRUNCATE TABLE condition_image");
        jdbcTemplate.execute("TRUNCATE TABLE condition_record");
        jdbcTemplate.execute("TRUNCATE TABLE rental_contract");
        jdbcTemplate.execute("TRUNCATE TABLE rental_product");
        jdbcTemplate.execute("TRUNCATE TABLE product_category");
        jdbcTemplate.execute("TRUNCATE TABLE rental_location");
        jdbcTemplate.execute("TRUNCATE TABLE member_account");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
