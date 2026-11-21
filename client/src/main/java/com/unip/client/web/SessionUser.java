package com.unip.client.web;

import java.io.Serializable;

public class SessionUser implements Serializable {

    public static final String KEY = "sessionUser";

    private final Long id;
    private final String name;
    private final String email;
    private final String role;
    private final String authorization; // Basic header, sent to the API on every call

    public SessionUser(Long id, String name, String email, String role, String authorization) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.authorization = authorization;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getAuthorization() { return authorization; }

    public boolean isEmployee() { return "EMPLOYEE".equals(role); }
    public boolean isOfficer() { return "ENVIRONMENTAL_OFFICER".equals(role); }
    public boolean isManager() { return "MANAGER".equals(role); }
}