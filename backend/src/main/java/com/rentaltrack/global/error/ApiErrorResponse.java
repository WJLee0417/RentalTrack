package com.rentaltrack.global.error;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        String code,
        String message,
        String path,
        LocalDateTime timestamp
) {
}
