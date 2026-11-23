package com.unip.client.web;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class IncidentForm {

    @NotBlank @Size(max = 120)
    private String title;

    @NotBlank @Size(max = 2000)
    private String description;

    @NotBlank
    private String type;

    @NotBlank
    private String severity;

    @NotNull
    private Long sectorId;

    @NotNull @PastOrPresent
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate occurredOn = LocalDate.now();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Long getSectorId() { return sectorId; }
    public void setSectorId(Long sectorId) { this.sectorId = sectorId; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }
}