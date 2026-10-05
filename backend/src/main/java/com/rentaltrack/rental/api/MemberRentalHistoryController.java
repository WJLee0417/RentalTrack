package com.rentaltrack.rental.api;

import com.rentaltrack.global.api.PageResponse;
import com.rentaltrack.rental.application.RentalHistoryService;
import com.rentaltrack.rental.domain.RentalContractStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@Validated
@RestController
@RequestMapping("/api/v1/members/{memberId}/rentals")
public class MemberRentalHistoryController {

    private final RentalHistoryService rentalHistoryService;

    public MemberRentalHistoryController(RentalHistoryService rentalHistoryService) {
        this.rentalHistoryService = rentalHistoryService;
    }

    @GetMapping
    public PageResponse<RentalHistoryItemResponse> getRentalHistory(
            @PathVariable @Positive Long memberId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return rentalHistoryService.getMemberRentalHistory(
                memberId,
                parseStatus(status),
                page,
                size
        );
    }

    private RentalContractStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return RentalContractStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown rental status: " + status);
        }
    }
}
