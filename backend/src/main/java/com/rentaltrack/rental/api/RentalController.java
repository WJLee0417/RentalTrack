package com.rentaltrack.rental.api;

import com.rentaltrack.access.application.AccessControlService;
import com.rentaltrack.rental.application.RentalApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @Valid @RequestBody RentalRequest request
    ) {
        accessControlService.requireSelfOrOperator(actorId, request.memberId());
        return rentalApplicationService.requestRental(request);
    }

    @PostMapping("/{rentalContractId}/pickup")
    public RentalResponse pickup(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long rentalContractId
    ) {
        accessControlService.requireRentalOwnerOrOperator(actorId, rentalContractId);
        return rentalApplicationService.pickup(rentalContractId);
    }

    @PostMapping("/{rentalContractId}/start")
    public RentalResponse startActualUse(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody StartRentalRequest request
    ) {
        accessControlService.requireRentalOwnerOrOperator(actorId, rentalContractId);
        return rentalApplicationService.startActualUse(rentalContractId, request);
    }

    @PostMapping("/{rentalContractId}/return")
    public RentalResponse returnRental(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody ReturnRequest request
    ) {
        accessControlService.requireRentalOwnerOrOperator(actorId, rentalContractId);
        return rentalApplicationService.returnRental(rentalContractId, request);
    }

    @GetMapping("/{rentalContractId}")
    public RentalResponse getRental(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long rentalContractId
    ) {
        accessControlService.requireRentalOwnerOrOperator(actorId, rentalContractId);
        return rentalApplicationService.getRental(rentalContractId);
    }
}
