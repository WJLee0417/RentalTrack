package com.rentaltrack.rental.domain;

import com.rentaltrack.location.domain.RentalLocation;
import com.rentaltrack.member.domain.Member;
import com.rentaltrack.product.domain.ProductCategory;
import com.rentaltrack.product.domain.RentalProduct;
import com.rentaltrack.product.domain.RentalProductStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RentalStateTransitionTest {

    @Test
    void contractFollowsExpectedLifecycle() {
        Fixture fixture = fixture();
        RentalContract contract = fixture.contract();
        RentalProduct product = fixture.product();

        LocalDateTime requestedAt = contract.getRequestedAt();
        LocalDateTime approvedAt = requestedAt.plusMinutes(1);
        LocalDateTime pickedUpAt = approvedAt.plusMinutes(1);
        LocalDateTime rentedAt = pickedUpAt.plusMinutes(1);
        LocalDateTime dueAt = rentedAt.plusHours(2);
        LocalDateTime returnedAt = rentedAt.plusHours(1);
        LocalDateTime completedAt = returnedAt.plusMinutes(10);

        contract.approve(approvedAt);
        product.reserve();

        contract.markPickedUp(pickedUpAt);
        product.markPickedUp();

        contract.startRental(rentedAt, dueAt);
        contract.markReturned(fixture.returnLocation(), returnedAt);
        product.markReturned(fixture.returnLocation());

        contract.beginInspection();
        contract.completeInspection(completedAt);
        product.completeInspection(false);

        assertThat(contract.getStatus()).isEqualTo(RentalContractStatus.COMPLETED);
        assertThat(product.getStatus()).isEqualTo(RentalProductStatus.AVAILABLE);
        assertThat(product.getCurrentLocation()).isSameAs(fixture.returnLocation());
        assertThat(contract.getReturnedAt()).isEqualTo(returnedAt);
        assertThat(contract.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void contractRejectsSkippedOrRepeatedTransitions() {
        Fixture fixture = fixture();
        RentalContract contract = fixture.contract();

        assertThatThrownBy(() -> contract.markPickedUp(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);

        contract.approve(LocalDateTime.now());

        assertThatThrownBy(() -> contract.approve(LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> contract.startRental(
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void actualUseRequiresDueAtAfterRentalStart() {
        Fixture fixture = fixture();
        RentalContract contract = fixture.contract();

        LocalDateTime now = LocalDateTime.now();
        contract.approve(now);
        contract.markPickedUp(now.plusMinutes(1));

        LocalDateTime rentedAt = now.plusMinutes(2);

        assertThatThrownBy(() -> contract.startRental(rentedAt, rentedAt))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void productCannotBeReturnedBeforeItIsRented() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.product().markReturned(fixture.returnLocation()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void newDamageInspectionMovesProductToMaintenance() {
        Fixture fixture = fixture();
        RentalProduct product = fixture.product();

        product.reserve();
        product.markPickedUp();
        product.markReturned(fixture.returnLocation());
        product.completeInspection(true);

        assertThat(product.getStatus()).isEqualTo(RentalProductStatus.MAINTENANCE);
    }

    private Fixture fixture() {
        RentalLocation pickup = new RentalLocation(
                "PICKUP",
                "Pickup",
                "Seoul",
                new BigDecimal("37.5000000"),
                new BigDecimal("127.0000000")
        );
        RentalLocation returned = new RentalLocation(
                "RETURN",
                "Return",
                "Seoul",
                new BigDecimal("37.5100000"),
                new BigDecimal("127.0100000")
        );
        ProductCategory category = new ProductCategory(
                "PORTABLE_ITEM",
                "Portable Item",
                "test"
        );
        RentalProduct product = new RentalProduct(
                "ASSET-1",
                "Portable Item A",
                category,
                pickup
        );
        Member member = new Member("member@example.com", "Member");
        RentalContract contract = new RentalContract(
                "RT-TEST",
                member,
                product,
                pickup,
                LocalDateTime.now().minusMinutes(5)
        );

        return new Fixture(contract, product, returned);
    }

    private record Fixture(
            RentalContract contract,
            RentalProduct product,
            RentalLocation returnLocation
    ) {
    }
}
