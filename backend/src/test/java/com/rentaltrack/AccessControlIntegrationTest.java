package com.rentaltrack;

import com.rentaltrack.auth.domain.AuthenticatedMember;
import com.rentaltrack.access.application.AccessControlService;
import com.rentaltrack.global.error.AccessDeniedException;
import com.rentaltrack.member.domain.MemberRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AccessControlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessControlService accessControlService;

    private Long ownerId;
    private Long otherUserId;
    private Long operatorId;
    private Long rentalId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        ownerId = insertMember("owner", "USER");
        otherUserId = insertMember("other", "USER");
        operatorId = insertMember("operator", "OPERATOR");

        Long locationId = insertLocation();
        Long categoryId = insertCategory();
        Long productId = insertProduct(locationId, categoryId);

        jdbcTemplate.update(
                """
                INSERT INTO rental_contract
                    (contract_code, member_id, rental_product_id, pickup_location_id,
                     status, requested_at, version)
                VALUES (?, ?, ?, ?, 'APPROVED', ?, 0)
                """,
                "RT-" + shortId(),
                ownerId,
                productId,
                locationId,
                LocalDateTime.now()
        );
        rentalId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM rental_contract",
                Long.class
        );
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void userCanAccessOwnRentalButNotAnotherUsersRental() {
        assertThatCode(() ->
                accessControlService.requireRentalOwnerOrOperator(
                        user(ownerId, "owner"),
                        rentalId
                )
        ).doesNotThrowAnyException();

        assertThatThrownBy(() ->
                accessControlService.requireRentalOwnerOrOperator(
                        user(otherUserId, "other"),
                        rentalId
                )
        ).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void operatorCanAccessAnotherUsersRentalAndOperatorOnlyAction() {
        AuthenticatedMember operator = operator();

        assertThatCode(() ->
                accessControlService.requireRentalOwnerOrOperator(
                        operator,
                        rentalId
                )
        ).doesNotThrowAnyException();

        assertThatCode(() ->
                accessControlService.requireOperator(operator)
        ).doesNotThrowAnyException();
    }

    @Test
    void normalUserCannotPerformOperatorOnlyAction() {
        assertThatThrownBy(() ->
                accessControlService.requireOperator(user(ownerId, "owner"))
        ).isInstanceOf(AccessDeniedException.class);
    }

    private AuthenticatedMember user(Long id, String name) {
        return new AuthenticatedMember(
                id,
                name + "@example.com",
                name,
                MemberRole.USER
        );
    }

    private AuthenticatedMember operator() {
        return new AuthenticatedMember(
                operatorId,
                "operator@example.com",
                "operator",
                MemberRole.OPERATOR
        );
    }

    private Long insertMember(String prefix, String role) {
        jdbcTemplate.update(
                """
                INSERT INTO member_account (email, name, status, role)
                VALUES (?, ?, 'ACTIVE', ?)
                """,
                prefix + "-" + UUID.randomUUID() + "@example.com",
                prefix,
                role
        );
        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM member_account",
                Long.class
        );
    }

    private Long insertLocation() {
        jdbcTemplate.update(
                """
                INSERT INTO rental_location
                    (code, name, address, latitude, longitude, active)
                VALUES (?, 'Access Station', 'Seoul', 37.5000000, 127.0000000, TRUE)
                """,
                "LOC-" + shortId()
        );
        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM rental_location",
                Long.class
        );
    }

    private Long insertCategory() {
        jdbcTemplate.update(
                """
                INSERT INTO product_category (code, name, description, active)
                VALUES (?, 'Access Item', 'test', TRUE)
                """,
                "CAT-" + shortId()
        );
        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM product_category",
                Long.class
        );
    }

    private Long insertProduct(Long locationId, Long categoryId) {
        jdbcTemplate.update(
                """
                INSERT INTO rental_product
                    (asset_code, display_name, category_id, current_location_id, status, version)
                VALUES (?, 'Access Product', ?, ?, 'AVAILABLE', 0)
                """,
                "ASSET-" + shortId(),
                categoryId,
                locationId
        );
        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM rental_product",
                Long.class
        );
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE inventory_recommendation");
        jdbcTemplate.execute("TRUNCATE TABLE demand_forecast");
        jdbcTemplate.execute("TRUNCATE TABLE model_run");
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
