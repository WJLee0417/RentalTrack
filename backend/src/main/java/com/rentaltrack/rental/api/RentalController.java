package com.rentaltrack.rental.api;

import com.rentaltrack.rental.application.RentalApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rentals")
public class RentalController {

    private final RentalApplicationService rentalApplicationService;

    public RentalController(RentalApplicationService rentalApplicationService) {
        this.rentalApplicationService = rentalApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentalResponse requestRental(@Valid @RequestBody RentalRequest request) {
        return rentalApplicationService.requestRental(request);
    }

    @PostMapping("/{rentalContractId}/pickup")
    public RentalResponse pickup(@PathVariable Long rentalContractId) {
        return rentalApplicationService.pickup(rentalContractId);
    }

    @PostMapping("/{rentalContractId}/start")
    public RentalResponse startActualUse(
            @PathVariable Long rentalContractId,
            @Valid @RequestBody StartRentalRequest request
    ) {
        return rentalApplicationService.startActualUse(rentalContractId, request);
    }

    @PostMapping("/{rentalContractId}/return")
    public RentalResponse returnRental(
            @PathVariable Long rentalContractId,
            @Valid @RequestBody ReturnRequest request
    ) {
        return rentalApplicationService.returnRental(rentalContractId, request);
    }

    @GetMapping("/{rentalContractId}")
    public RentalResponse getRental(@PathVariable Long rentalContractId) {
        return rentalApplicationService.getRental(rentalContractId);
    }
}
