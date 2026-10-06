package com.rentaltrack.damage.api;

import com.rentaltrack.auth.domain.AuthenticatedMember;

import com.rentaltrack.access.application.AccessControlService;
import com.rentaltrack.damage.application.DamageApplicationService;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/rentals/{rentalContractId}/damages")
public class DamageController {

    private final DamageApplicationService damageApplicationService;
    private final AccessControlService accessControlService;

    public DamageController(
            DamageApplicationService damageApplicationService,
            AccessControlService accessControlService
    ) {
        this.damageApplicationService = damageApplicationService;
        this.accessControlService = accessControlService;
    }

    @PostMapping("/before")
    @ResponseStatus(HttpStatus.CREATED)
    public DamageResponse addBeforeDamage(
            @AuthenticationPrincipal AuthenticatedMember actor,
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody DamageCreateRequest request
    ) {
        accessControlService.requireRentalOwnerOrOperator(actor, rentalContractId);
        return damageApplicationService.addBeforeDamage(rentalContractId, request);
    }

    @PostMapping("/after")
    @ResponseStatus(HttpStatus.CREATED)
    public DamageResponse addAfterDamage(
            @AuthenticationPrincipal AuthenticatedMember actor,
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody DamageCreateRequest request
    ) {
        accessControlService.requireRentalOwnerOrOperator(actor, rentalContractId);
        return damageApplicationService.addAfterDamage(rentalContractId, request);
    }

    @PostMapping("/compare")
    public List<DamageResponse> compare(
            @AuthenticationPrincipal AuthenticatedMember actor,
            @PathVariable @Positive Long rentalContractId
    ) {
        accessControlService.requireOperator(actor);
        return damageApplicationService.compare(rentalContractId);
    }

    @GetMapping("/after")
    public List<DamageResponse> getAfterDamages(
            @AuthenticationPrincipal AuthenticatedMember actor,
            @PathVariable @Positive Long rentalContractId
    ) {
        accessControlService.requireRentalOwnerOrOperator(actor, rentalContractId);
        return damageApplicationService.getAfterDamages(rentalContractId);
    }
}
