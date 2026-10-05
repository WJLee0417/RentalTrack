package com.rentaltrack.inspection.api;

import com.rentaltrack.inspection.application.ReturnInspectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
@RequestMapping("/api/v1/rentals/{rentalContractId}/inspection")
public class ReturnInspectionController {

    private final ReturnInspectionService returnInspectionService;

    public ReturnInspectionController(ReturnInspectionService returnInspectionService) {
        this.returnInspectionService = returnInspectionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnInspectionResponse inspect(
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody ReturnInspectionRequest request
    ) {
        return returnInspectionService.inspect(rentalContractId, request);
    }

    @GetMapping
    public ReturnInspectionResponse getInspection(@PathVariable @Positive Long rentalContractId) {
        return returnInspectionService.getInspection(rentalContractId);
    }
}
