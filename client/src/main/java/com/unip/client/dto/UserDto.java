package com.unip.client.dto;

public record UserDto(Long id, String name, String email, String role, boolean active) {}