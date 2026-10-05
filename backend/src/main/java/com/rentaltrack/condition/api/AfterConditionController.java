package com.rentaltrack.condition.api;

import com.rentaltrack.condition.application.ConditionApplicationService;
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
@RequestMapping("/api/v1/rentals/{rentalContractId}/conditions/after")
public class AfterConditionController {

    private final ConditionApplicationService conditionApplicationService;

    public AfterConditionController(ConditionApplicationService conditionApplicationService) {
        this.conditionApplicationService = conditionApplicationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConditionRecordResponse createAfterCondition(
            @PathVariable Long rentalContractId,
            @Valid @RequestBody AfterConditionRequest request
    ) {
        return conditionApplicationService.createAfterCondition(rentalContractId, request);
    }

    @PostMapping("/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ConditionImageResponse addImage(
            @PathVariable Long rentalContractId,
            @Valid @RequestBody ConditionImageRequest request
    ) {
        return conditionApplicationService.addAfterConditionImage(rentalContractId, request);
    }

    @GetMapping
    public ConditionRecordResponse getAfterCondition(@PathVariable Long rentalContractId) {
        return conditionApplicationService.getAfterCondition(rentalContractId);
    }
}
