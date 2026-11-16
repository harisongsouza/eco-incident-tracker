package com.unip.api.check.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckRequest(
        @NotNull Boolean effective,
        @NotBlank @Size(max = 1000) String opinion
) {}