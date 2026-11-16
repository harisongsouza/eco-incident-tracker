package com.unip.api.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DismissRequest(@NotBlank @Size(max = 400) String justification) {}