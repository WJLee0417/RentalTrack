package com.rentaltrack;

import com.rentaltrack.global.api.PageResponse;
import com.rentaltrack.rental.api.RentalHistoryItemResponse;
import com.rentaltrack.rental.application.RentalHistoryService;
import com.rentaltrack.rental.domain.RentalContractStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RentalHistoryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RentalHistoryService rentalHistoryService;

    private Long memberId;
    private Long locationId;
    private Long categoryId;
    private Long firstProductId;
    private Long secondProductId;
    private Long thirdProductId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        jdbcTemplate.update(
                "INSERT INTO member_account (email, name, status) VALUES (?, 'History Tester', 'ACTIVE')",
                "history-" + UUID.randomUUID() + "@example.com"
        );
        memberId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM member_account", Long.class);

        jdbcTemplate.update(
                """
                INSERT INTO rental_location
                    (code, name, address, latitude, longitude, active)
                VALUES (?, 'History Station', 'Seoul', 37.5000000, 127.0000000, TRUE)
                """,
                "LOC-" + shortId()
        );
        locationId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM rental_location", Long.class);

        jdbcTemplate.update(
                """
                INSERT INTO product_category (code, name, description, active)
                VALUES (?, 'Portable Item', 'history category', TRUE)
                """,
                "CAT-" + shortId()
        );
        categoryId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM product_category", Long.class);

        firstProductId = insertProduct("Product A");
        secondProductId = insertProduct("Product B");
        thirdProductId = insertProduct("Product C");

        insertContract(
                "RT-" + shortId(),
                firstProductId,
                "COMPLETED",
                LocalDateTime.now().minusDays(3)
        );
        insertContract(
                "RT-" + shortId(),
                secondProductId,
                "IN_USE",
                LocalDateTime.now().minusDays(2)
        );
        insertContract(
                "RT-" + shortId(),
                thirdProductId,
                "COMPLETED",
                LocalDateTime.now().minusDays(1)
        );
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void historyIsReturnedLatestFirstWithPagination() {
        PageResponse<RentalHistoryItemResponse> firstPage =
                rentalHistoryService.getMemberRentalHistory(memberId, null, 0, 2);

        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.content())
                .extracting(RentalHistoryItemResponse::productName)
                .containsExactly("Product C", "Product B");

        PageResponse<RentalHistoryItemResponse> secondPage =
                rentalHistoryService.getMemberRentalHistory(memberId, null, 1, 2);

        assertThat(secondPage.content())
                .extracting(RentalHistoryItemResponse::productName)
                .containsExactly("Product A");
        assertThat(secondPage.hasPrevious()).isTrue();
    }

    @Test
    void historyCanBeFilteredByStatus() {
        PageResponse<RentalHistoryItemResponse> completed =
                rentalHistoryService.getMemberRentalHistory(
                        memberId,
                        RentalContractStatus.COMPLETED,
                        0,
                        20
                );

        assertThat(completed.totalElements()).isEqualTo(2);
        assertThat(completed.content())
                .extracting(RentalHistoryItemResponse::status)
                .containsOnly("COMPLETED");
    }

    private Long insertProduct(String name) {
        jdbcTemplate.update(
                """
                INSERT INTO rental_product
                    (asset_code, display_name, category_id, current_location_id, status, version)
                VALUES (?, ?, ?, ?, 'AVAILABLE', 0)
                """,
                "ASSET-" + shortId(),
                name,
                categoryId,
                locationId
        );
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM rental_product", Long.class);
    }

    private void insertContract(
            String contractCode,
            Long productId,
            String status,
            LocalDateTime requestedAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO rental_contract
                    (contract_code, member_id, rental_product_id, pickup_location_id,
                     status, requested_at, version)
                VALUES (?, ?, ?, ?, ?, ?, 0)
                """,
                contractCode,
                memberId,
                productId,
                locationId,
                status,
                requestedAt
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
