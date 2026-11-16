package com.unip.api.action.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteActionRequest(@NotBlank @Size(max = 500) String note) {}