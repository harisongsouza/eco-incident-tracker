package com.unip.api.user.dto;

import com.unip.api.user.Role;
import com.unip.api.user.User;

public record UserResponse(Long id, String name, String email, Role role, boolean active) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isActive());
    }
}