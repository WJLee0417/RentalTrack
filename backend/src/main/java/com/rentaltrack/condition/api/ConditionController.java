package com.rentaltrack.condition.api;

import com.rentaltrack.condition.application.ConditionApplicationService;
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
@RequestMapping("/api/v1/rentals/{rentalContractId}/conditions/before")
public class ConditionController {

    private final ConditionApplicationService conditionApplicationService;

    public ConditionController(ConditionApplicationService conditionApplicationService) {
        this.conditionApplicationService = conditionApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConditionRecordResponse createBeforeCondition(
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody BeforeConditionRequest request
    ) {
        return conditionApplicationService.createBeforeCondition(rentalContractId, request);
    }

    @PostMapping("/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ConditionImageResponse addImage(
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody ConditionImageRequest request
    ) {
        return conditionApplicationService.addBeforeConditionImage(rentalContractId, request);
    }

    @GetMapping
    public ConditionRecordResponse getBeforeCondition(@PathVariable @Positive Long rentalContractId) {
        return conditionApplicationService.getBeforeCondition(rentalContractId);
    }
}
