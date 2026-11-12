package com.unip.api.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RootCauseRequest(@NotBlank @Size(max = 2000) String rootCause) {}