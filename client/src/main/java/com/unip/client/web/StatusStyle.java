package com.unip.client.web;

import org.springframework.stereotype.Component;

@Component("statusStyle")
public class StatusStyle {

    public String status(String status) {
        return switch (status) {
            case "OPEN" -> "bg-secondary";
            case "UNDER_ANALYSIS" -> "bg-info text-dark";
            case "IN_PROGRESS" -> "bg-primary";
            case "AWAITING_VERIFICATION" -> "bg-warning text-dark";
            case "CLOSED" -> "bg-success";
            case "DISMISSED" -> "bg-dark";
            default -> "bg-light text-dark";
        };
    }

    public String severity(String severity) {
        return switch (severity) {
            case "LOW" -> "bg-success";
            case "MEDIUM" -> "bg-warning text-dark";
            case "HIGH" -> "bg-danger";
            case "CRITICAL" -> "bg-dark";
            default -> "bg-light text-dark";
        };
    }
}