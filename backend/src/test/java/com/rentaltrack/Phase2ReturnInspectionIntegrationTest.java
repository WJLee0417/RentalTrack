package com.rentaltrack;

import com.rentaltrack.catalog.application.CatalogService;
import com.rentaltrack.condition.api.AfterConditionRequest;
import com.rentaltrack.condition.api.BeforeConditionRequest;
import com.rentaltrack.condition.api.ConditionImageMetadataRequest;
import com.rentaltrack.condition.application.ConditionApplicationService;
import com.rentaltrack.damage.api.DamageCreateRequest;
import com.rentaltrack.damage.api.DamageResponse;
import com.rentaltrack.damage.application.DamageApplicationService;
import com.rentaltrack.damage.domain.DamageSeverity;
import com.rentaltrack.damage.domain.DamageType;
import com.rentaltrack.inspection.api.ReturnInspectionRequest;
import com.rentaltrack.inspection.application.ReturnInspectionService;
import com.rentaltrack.inspection.domain.ReturnInspectionResult;
import com.rentaltrack.product.domain.RentalProductStatus;
import com.rentaltrack.rental.api.RentalRequest;
import com.rentaltrack.rental.api.RentalResponse;
import com.rentaltrack.rental.api.ReturnRequest;
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

@SpringBootTest
class Phase2ReturnInspectionIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private RentalApplicationService rentalApplicationService;

    @Autowired
    private ConditionApplicationService conditionApplicationService;

    @Autowired
    private DamageApplicationService damageApplicationService;

    @Autowired
    private ReturnInspectionService returnInspectionService;

    private Long memberId;
    private Long pickupLocationId;
    private Long returnLocationId;
    private Long categoryId;
    private Long productId;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        jdbcTemplate.update(
                "INSERT INTO member_account (email, name, status) VALUES (?, 'Phase2 Return Tester', 'ACTIVE')",
                "phase2-return-" + UUID.randomUUID() + "@example.com"
        );
        memberId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM member_account", Long.class);

        pickupLocationId = insertLocation("Pickup Station");
        returnLocationId = insertLocation("Return Station");

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
                pickupLocationId
        );
        productId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM rental_product", Long.class);
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void returnAfterConditionDamageComparisonAndInspectionCompleteFlow() {
        RentalResponse rental = startRentalWithBeforeEvidence();

        RentalResponse returned = rentalApplicationService.returnRental(
                rental.id(),
                new ReturnRequest(returnLocationId)
        );

        assertThat(returned.status()).isEqualTo("RETURNED");
        assertThat(returned.returnLocationId()).isEqualTo(returnLocationId);
        assertThat(productStatus()).isEqualTo(RentalProductStatus.INSPECTION.name());
        assertThat(productLocation()).isEqualTo(returnLocationId);

        conditionApplicationService.createAfterCondition(
                rental.id(),
                new AfterConditionRequest("Returned with one additional crack")
        );
        conditionApplicationService.addAfterConditionImage(
                rental.id(),
                new ConditionImageMetadataRequest(
                        1,
                        "FRONT",
                        LocalDateTime.now()
                ),
                imageFile("after.jpg")
        );

        damageApplicationService.addAfterDamage(
                rental.id(),
                new DamageCreateRequest(
                        DamageType.SCRATCH,
                        "left_side",
                        DamageSeverity.MINOR,
                        "same scratch"
                )
        );
        damageApplicationService.addAfterDamage(
                rental.id(),
                new DamageCreateRequest(
                        DamageType.CRACK,
                        "handle",
                        DamageSeverity.MODERATE,
                        "new crack"
                )
        );

        List<DamageResponse> compared = damageApplicationService.compare(rental.id());

        assertThat(compared)
                .extracting(DamageResponse::comparisonResult)
                .containsExactly("EXISTING", "NEW");

        var inspection = returnInspectionService.inspect(
                rental.id(),
                new ReturnInspectionRequest(
                        ReturnInspectionResult.NEW_DAMAGE_CONFIRMED,
                        "New handle crack confirmed"
                )
        );

        assertThat(inspection.result()).isEqualTo("NEW_DAMAGE_CONFIRMED");
        assertThat(contractStatus(rental.id())).isEqualTo("COMPLETED");
        assertThat(productStatus()).isEqualTo(RentalProductStatus.MAINTENANCE.name());
    }

    @Test
    void noNewDamageReturnsProductToAvailable() {
        RentalResponse rental = startRentalWithBeforeEvidence();

        rentalApplicationService.returnRental(
                rental.id(),
                new ReturnRequest(returnLocationId)
        );

        conditionApplicationService.createAfterCondition(
                rental.id(),
                new AfterConditionRequest("No additional damage")
        );
        conditionApplicationService.addAfterConditionImage(
                rental.id(),
                new ConditionImageMetadataRequest(
                        1,
                        "FRONT",
                        LocalDateTime.now()
                ),
                imageFile("after.jpg")
        );
        damageApplicationService.addAfterDamage(
                rental.id(),
                new DamageCreateRequest(
                        DamageType.SCRATCH,
                        "LEFT_SIDE",
                        DamageSeverity.MINOR,
                        "same scratch"
                )
        );

        damageApplicationService.compare(rental.id());

        returnInspectionService.inspect(
                rental.id(),
                new ReturnInspectionRequest(
                        ReturnInspectionResult.NO_NEW_DAMAGE,
                        "No new damage"
                )
        );

        assertThat(contractStatus(rental.id())).isEqualTo("COMPLETED");
        assertThat(productStatus()).isEqualTo(RentalProductStatus.AVAILABLE.name());
    }

    private RentalResponse startRentalWithBeforeEvidence() {
        assertThat(catalogService.findAvailableProducts(pickupLocationId, categoryId)).hasSize(1);

        RentalResponse requested = rentalApplicationService.requestRental(
                new RentalRequest(memberId, productId, pickupLocationId)
        );
        rentalApplicationService.pickup(requested.id());

        conditionApplicationService.createBeforeCondition(
                requested.id(),
                new BeforeConditionRequest("Existing left side scratch")
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

        damageApplicationService.addBeforeDamage(
                requested.id(),
                new DamageCreateRequest(
                        DamageType.SCRATCH,
                        "LEFT_SIDE",
                        DamageSeverity.MINOR,
                        "existing scratch"
                )
        );

        return rentalApplicationService.startActualUse(
                requested.id(),
                new StartRentalRequest(LocalDateTime.now().plusHours(2))
        );
    }

    private MockMultipartFile imageFile(String name) {
        return new MockMultipartFile(
                "file",
                name,
                "image/jpeg",
                ("image-" + shortId()).getBytes()
        );
    }

    private Long insertLocation(String name) {
        jdbcTemplate.update(
                """
                INSERT INTO rental_location
                    (code, name, address, latitude, longitude, active)
                VALUES (?, ?, 'Seoul', 37.5000000, 127.0000000, TRUE)
                """,
                "LOC-" + shortId(),
                name
        );
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM rental_location", Long.class);
    }

    private String contractStatus(Long rentalId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM rental_contract WHERE id = ?",
                String.class,
                rentalId
        );
    }

    private String productStatus() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM rental_product WHERE id = ?",
                String.class,
                productId
        );
    }

    private Long productLocation() {
        return jdbcTemplate.queryForObject(
                "SELECT current_location_id FROM rental_product WHERE id = ?",
                Long.class,
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
