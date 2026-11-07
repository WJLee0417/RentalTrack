package com.rentaltrack.auth.api;

import jakarta.validation.constraints.*;

public record LoginRequest(
  @NotBlank @Size(max = 50) String loginId,
  @NotBlank @Size(max = 72) String password
) {}
