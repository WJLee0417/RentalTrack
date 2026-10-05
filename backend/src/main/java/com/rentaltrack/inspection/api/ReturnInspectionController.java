package com.rentaltrack.inspection.api;

import com.rentaltrack.access.application.AccessControlService;
import com.rentaltrack.inspection.application.ReturnInspectionService;
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
@RequestMapping("/api/v1/rentals/{rentalContractId}/inspection")
public class ReturnInspectionController {

    private final ReturnInspectionService returnInspectionService;
    private final AccessControlService accessControlService;

    public ReturnInspectionController(
            ReturnInspectionService returnInspectionService,
            AccessControlService accessControlService
    ) {
        this.returnInspectionService = returnInspectionService;
        this.accessControlService = accessControlService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnInspectionResponse inspect(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody ReturnInspectionRequest request
    ) {
        accessControlService.requireOperator(actorId);
        return returnInspectionService.inspect(rentalContractId, request);
    }

    @GetMapping
    public ReturnInspectionResponse getInspection(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long rentalContractId
    ) {
        accessControlService.requireOperator(actorId);
        return returnInspectionService.getInspection(rentalContractId);
    }
}
