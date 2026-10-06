package com.rentaltrack.rental.api;

import com.rentaltrack.auth.domain.AuthenticatedMember;

import com.rentaltrack.access.application.AccessControlService;
import com.rentaltrack.rental.application.RentalApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/rentals")
public class RentalController {

    private final RentalApplicationService rentalApplicationService;
    private final AccessControlService accessControlService;

    public RentalController(
            RentalApplicationService rentalApplicationService,
            AccessControlService accessControlService
    ) {
        this.rentalApplicationService = rentalApplicationService;
        this.accessControlService = accessControlService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentalResponse requestRental(
            @AuthenticationPrincipal AuthenticatedMember actor,
            @Valid @RequestBody RentalRequest request
    ) {
        accessControlService.requireSelfOrOperator(actor, request.memberId());
        return rentalApplicationService.requestRental(request);
    }

    @PostMapping("/{rentalContractId}/pickup")
    public RentalResponse pickup(
            @AuthenticationPrincipal AuthenticatedMember actor,
            @PathVariable @Positive Long rentalContractId
    ) {
        accessControlService.requireRentalOwnerOrOperator(actor, rentalContractId);
        return rentalApplicationService.pickup(rentalContractId);
    }

    @PostMapping("/{rentalContractId}/start")
    public RentalResponse startActualUse(
            @AuthenticationPrincipal AuthenticatedMember actor,
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody StartRentalRequest request
    ) {
        accessControlService.requireRentalOwnerOrOperator(actor, rentalContractId);
        return rentalApplicationService.startActualUse(rentalContractId, request);
    }

    @PostMapping("/{rentalContractId}/return")
    public RentalResponse returnRental(
            @AuthenticationPrincipal AuthenticatedMember actor,
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody ReturnRequest request
    ) {
        accessControlService.requireRentalOwnerOrOperator(actor, rentalContractId);
        return rentalApplicationService.returnRental(rentalContractId, request);
    }

    @GetMapping("/{rentalContractId}")
    public RentalResponse getRental(
            @AuthenticationPrincipal AuthenticatedMember actor,
            @PathVariable @Positive Long rentalContractId
    ) {
        accessControlService.requireRentalOwnerOrOperator(actor, rentalContractId);
        return rentalApplicationService.getRental(rentalContractId);
    }
}
