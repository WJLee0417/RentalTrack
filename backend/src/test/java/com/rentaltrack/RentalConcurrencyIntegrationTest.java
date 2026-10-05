package com.rentaltrack;

import com.rentaltrack.rental.api.RentalRequest;
import com.rentaltrack.rental.application.RentalApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RentalConcurrencyIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RentalApplicationService rentalApplicationService;

    private Long firstMemberId;
    private Long secondMemberId;
    private Long locationId;
    private Long categoryId;
    private Long productId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        firstMemberId = insertMember("first");
        secondMemberId = insertMember("second");

        jdbcTemplate.update(
                """
                INSERT INTO rental_location
                    (code, name, address, latitude, longitude, active)
                VALUES (?, 'Concurrency Station', 'Seoul', 37.5000000, 127.0000000, TRUE)
                """,
                "LOC-" + shortId()
        );
        locationId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM rental_location",
                Long.class
        );

        jdbcTemplate.update(
                """
                INSERT INTO product_category (code, name, description, active)
                VALUES (?, 'Concurrency Item', 'test category', TRUE)
                """,
                "CAT-" + shortId()
        );
        categoryId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM product_category",
                Long.class
        );

        jdbcTemplate.update(
                """
                INSERT INTO rental_product
                    (asset_code, display_name, category_id, current_location_id, status, version)
                VALUES (?, 'Single Product', ?, ?, 'AVAILABLE', 0)
                """,
                "ASSET-" + shortId(),
                categoryId,
                locationId
        );
        productId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM rental_product",
                Long.class
        );
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void onlyOneConcurrentRequestCanReserveTheSameProduct() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Result> first = executor.submit(() ->
                    request(firstMemberId, ready, start)
            );
            Future<Result> second = executor.submit(() ->
                    request(secondMemberId, ready, start)
            );

            ready.await();
            start.countDown();

            List<Result> results = List.of(first.get(), second.get());

            assertThat(results.stream().filter(Result::success).count()).isEqualTo(1);
            assertThat(results.stream().filter(result -> !result.success()).count()).isEqualTo(1);
            assertThat(results.stream()
                    .filter(result -> !result.success())
                    .map(Result::exceptionType))
                    .containsExactly(IllegalStateException.class.getName());
        }

        Integer contractCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rental_contract WHERE rental_product_id = ?",
                Integer.class,
                productId
        );
        String productStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM rental_product WHERE id = ?",
                String.class,
                productId
        );

        assertThat(contractCount).isEqualTo(1);
        assertThat(productStatus).isEqualTo("RESERVED");
    }

    private Result request(
            Long memberId,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        try {
            ready.countDown();
            start.await();

            rentalApplicationService.requestRental(
                    new RentalRequest(memberId, productId, locationId)
            );
            return new Result(true, null);
        } catch (Exception exception) {
            return new Result(false, exception.getClass().getName());
        }
    }

    private Long insertMember(String prefix) {
        jdbcTemplate.update(
                "INSERT INTO member_account (email, name, status) VALUES (?, ?, 'ACTIVE')",
                prefix + "-" + UUID.randomUUID() + "@example.com",
                prefix
        );
        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM member_account",
                Long.class
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

    private record Result(boolean success, String exceptionType) {
    }
}
