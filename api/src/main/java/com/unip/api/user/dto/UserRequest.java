package com.unip.api.user.dto;

import com.unip.api.user.Role;
import jakarta.validation.constraints.*;

public record UserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role
) {}