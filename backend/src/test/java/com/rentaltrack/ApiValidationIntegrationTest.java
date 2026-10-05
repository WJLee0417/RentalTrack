package com.rentaltrack;

import com.rentaltrack.condition.api.ConditionImageRequest;
import com.rentaltrack.rental.api.RentalRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApiValidationIntegrationTest {

    @Autowired
    private Validator validator;

    @Test
    void rentalRequestRejectsNonPositiveIdentifiers() {
        RentalRequest request = new RentalRequest(0L, -1L, null);

        Set<ConstraintViolation<RentalRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("memberId", "rentalProductId", "pickupLocationId");
    }

    @Test
    void conditionImageRejectsFutureCaptureAndInvalidMetadata() {
        ConditionImageRequest request = new ConditionImageRequest(
                " ",
                " ",
                0,
                "V".repeat(31),
                LocalDateTime.now().plusMinutes(10)
        );

        Set<ConstraintViolation<ConditionImageRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("imageUrl", "storageKey", "captureOrder", "viewpoint", "capturedAt");
    }
}
