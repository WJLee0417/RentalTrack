package com.rentaltrack.auth.api;

import jakarta.validation.constraints.*;

public record RegisterRequest(
  @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}") String loginId,
  @NotBlank @Email @Size(max = 254) String email,
  @NotBlank @Size(min = 10, max = 72) String password,
  @NotBlank @Size(max = 100) String name
) {}
