package com.rentaltrack.damage.api;

import com.rentaltrack.damage.application.DamageApplicationService;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/rentals/{rentalContractId}/damages")
public class DamageController {

    private final DamageApplicationService damageApplicationService;

    public DamageController(DamageApplicationService damageApplicationService) {
        this.damageApplicationService = damageApplicationService;
    }

    @PostMapping("/before")
    @ResponseStatus(HttpStatus.CREATED)
    public DamageResponse addBeforeDamage(
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody DamageCreateRequest request
    ) {
        return damageApplicationService.addBeforeDamage(rentalContractId, request);
    }

    @PostMapping("/after")
    @ResponseStatus(HttpStatus.CREATED)
    public DamageResponse addAfterDamage(
            @PathVariable @Positive Long rentalContractId,
            @Valid @RequestBody DamageCreateRequest request
    ) {
        return damageApplicationService.addAfterDamage(rentalContractId, request);
    }

    @PostMapping("/compare")
    public List<DamageResponse> compare(@PathVariable @Positive Long rentalContractId) {
        return damageApplicationService.compare(rentalContractId);
    }

    @GetMapping("/after")
    public List<DamageResponse> getAfterDamages(@PathVariable @Positive Long rentalContractId) {
        return damageApplicationService.getAfterDamages(rentalContractId);
    }
}
