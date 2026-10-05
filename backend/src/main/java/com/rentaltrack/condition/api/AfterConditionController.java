package com.rentaltrack.condition.api;

import com.rentaltrack.access.application.AccessControlService;
import com.rentaltrack.condition.application.ConditionApplicationService;
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
@RequestMapping("/api/v1/rentals/{rentalContractId}/conditions/after")
public class AfterConditionController {

    private final ConditionApplicationService conditionApplicationService;
    private final AccessControlService accessControlService;

    public AfterConditionController(
            ConditionApplicationService conditionApplicationService,
            AccessControlService accessControlService
    ) {
        this.conditionApplicationService = conditionApplicationService;
        this.accessControlService = accessControlService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConditionRecordResponse createAfterCondition(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody AfterConditionRequest request
    ) {
        accessControlService.requireRentalOwnerOrOperator(actorId, rentalContractId);
        return conditionApplicationService.createAfterCondition(rentalContractId, request);
    }

    @PostMapping("/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ConditionImageResponse addImage(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody ConditionImageRequest request
    ) {
        accessControlService.requireRentalOwnerOrOperator(actorId, rentalContractId);
        return conditionApplicationService.addAfterConditionImage(rentalContractId, request);
    }

    @GetMapping
    public ConditionRecordResponse getAfterCondition(
            @RequestHeader(name = "X-Actor-Id", required = false) @Positive Long actorId,
            @PathVariable @Positive Long rentalContractId
    ) {
        accessControlService.requireRentalOwnerOrOperator(actorId, rentalContractId);
        return conditionApplicationService.getAfterCondition(rentalContractId);
    }
}
