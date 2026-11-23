package com.unip.client.api;

import java.time.LocalDate;

/** Request bodies sent to the API (same JSON field names as the API DTOs). */
public final class ApiRequests {

    private ApiRequests() {}

    public record Incident(String title, String description, String type,
                           String severity, Long sectorId, LocalDate occurredOn) {}

    public record RootCause(String rootCause) {}

    public record Action(String description, Long responsibleId, LocalDate deadline) {}

    public record CompleteAction(String note) {}

    public record Check(boolean effective, String opinion) {}

    public record Dismiss(String justification) {}
}