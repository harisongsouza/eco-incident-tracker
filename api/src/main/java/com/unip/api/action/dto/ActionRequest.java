package com.unip.api.action.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ActionRequest(
        @NotBlank @Size(max = 500) String description,
        @NotBlank @Size(max = 120) String responsible,
        @NotNull @Future LocalDate deadline
) {}