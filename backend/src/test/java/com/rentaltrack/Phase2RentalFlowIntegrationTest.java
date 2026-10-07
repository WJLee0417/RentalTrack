package com.rentaltrack;

import com.rentaltrack.catalog.api.ProductResponse;
import com.rentaltrack.catalog.application.CatalogService;
import com.rentaltrack.condition.api.BeforeConditionRequest;
import com.rentaltrack.condition.api.ConditionImageMetadataRequest;
import com.rentaltrack.condition.application.ConditionApplicationService;
import com.rentaltrack.product.domain.RentalProductStatus;
import com.rentaltrack.rental.api.RentalRequest;
import com.rentaltrack.rental.api.RentalResponse;
import com.rentaltrack.rental.api.StartRentalRequest;
import com.rentaltrack.rental.application.RentalApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class Phase2RentalFlowIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private RentalApplicationService rentalApplicationService;

    @Autowired
    private ConditionApplicationService conditionApplicationService;

    private Long memberId;
    private Long locationId;
    private Long categoryId;
    private Long productId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        jdbcTemplate.update(
                "INSERT INTO member_account (email, name, status) VALUES (?, 'Phase2 Tester', 'ACTIVE')",
                "phase2-" + UUID.randomUUID() + "@example.com"
        );
        memberId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM member_account", Long.class);

        jdbcTemplate.update(
                """
                INSERT INTO rental_location
                    (code, name, address, latitude, longitude, active)
                VALUES (?, 'Station A', 'Seoul', 37.5000000, 127.0000000, TRUE)
                """,
                "LOC-" + shortId()
        );
        locationId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM rental_location", Long.class);

        jdbcTemplate.update(
                """
                INSERT INTO product_category (code, name, description, active)
                VALUES (?, 'Portable Charger', 'urgent-use item', TRUE)
                """,
                "CAT-" + shortId()
        );
        categoryId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM product_category", Long.class);

        jdbcTemplate.update(
                """
                INSERT INTO rental_product
                    (asset_code, display_name, category_id, current_location_id, status, version)
                VALUES (?, 'Portable Charger A', ?, ?, 'AVAILABLE', 0)
                """,
                "ASSET-" + shortId(),
                categoryId,
                locationId
        );
        productId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM rental_product", Long.class);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void searchRequestPickupRecordBeforeConditionAndStartActualUse() {
        List<ProductResponse> products = catalogService.findAvailableProducts(locationId, categoryId);
        assertThat(products).hasSize(1);
        assertThat(products.getFirst().id()).isEqualTo(productId);

        RentalResponse requested = rentalApplicationService.requestRental(
                new RentalRequest(memberId, productId, locationId)
        );
        assertThat(requested.status()).isEqualTo("APPROVED");
        assertThat(productStatus()).isEqualTo(RentalProductStatus.RESERVED.name());

        RentalResponse pickedUp = rentalApplicationService.pickup(requested.id());
        assertThat(pickedUp.status()).isEqualTo("PICKED_UP");
        assertThat(pickedUp.pickedUpAt()).isNotNull();
        assertThat(productStatus()).isEqualTo(RentalProductStatus.RENTED.name());

        var before = conditionApplicationService.createBeforeCondition(
                requested.id(),
                new BeforeConditionRequest("No visible damage")
        );

        conditionApplicationService.addBeforeConditionImage(
                requested.id(),
                new ConditionImageMetadataRequest(
                        1,
                        "FRONT",
                        LocalDateTime.now()
                ),
                imageFile("before.jpg")
        );

        assertThat(before.recordType()).isEqualTo("BEFORE_RENTAL");

        LocalDateTime dueAt = LocalDateTime.now().plusHours(2);
        RentalResponse started = rentalApplicationService.startActualUse(
                requested.id(),
                new StartRentalRequest(dueAt)
        );

        assertThat(started.status()).isEqualTo("IN_USE");
        assertThat(started.rentedAt()).isNotNull();
        assertThat(started.dueAt()).isEqualTo(dueAt);
    }

    @Test
    void actualUseCannotStartWithoutBeforeConditionImage() {
        RentalResponse requested = rentalApplicationService.requestRental(
                new RentalRequest(memberId, productId, locationId)
        );

        rentalApplicationService.pickup(requested.id());

        conditionApplicationService.createBeforeCondition(
                requested.id(),
                new BeforeConditionRequest("record exists but no image")
        );

        assertThatThrownBy(() -> rentalApplicationService.startActualUse(
                requested.id(),
                new StartRentalRequest(LocalDateTime.now().plusHours(1))
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void beforeConditionCannotBeRecordedBeforePickup() {
        RentalResponse requested = rentalApplicationService.requestRental(
                new RentalRequest(memberId, productId, locationId)
        );

        assertThatThrownBy(() -> conditionApplicationService.createBeforeCondition(
                requested.id(),
                new BeforeConditionRequest("too early")
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void secondRentalRequestForSameProductIsRejected() {
        rentalApplicationService.requestRental(
                new RentalRequest(memberId, productId, locationId)
        );

        assertThatThrownBy(() -> rentalApplicationService.requestRental(
                new RentalRequest(memberId, productId, locationId)
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pickupCannotBeRepeated() {
        RentalResponse requested = rentalApplicationService.requestRental(
                new RentalRequest(memberId, productId, locationId)
        );

        rentalApplicationService.pickup(requested.id());

        assertThatThrownBy(() -> rentalApplicationService.pickup(requested.id()))
                .isInstanceOf(IllegalStateException.class);
    }

    private MockMultipartFile imageFile(String name) {
        return new MockMultipartFile(
                "file",
                name,
                "image/jpeg",
                ("image-" + shortId()).getBytes()
        );
    }

    private String productStatus() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM rental_product WHERE id = ?",
                String.class,
                productId
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
