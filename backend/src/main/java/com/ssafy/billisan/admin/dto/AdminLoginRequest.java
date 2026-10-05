package com.ssafy.billisan.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank String loginId,
        @NotBlank String password
) {
}
