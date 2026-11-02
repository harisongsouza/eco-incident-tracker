package com.unip.api.incident;

import com.unip.api.sector.Sector;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @ManyToOne(optional = false)
    private Sector sector;

    @Column(nullable = false)
    private LocalDate occurredOn;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    @Column(nullable = false)
    private int cycle;

    protected Incident() {}

    public Incident(String title, String description, IncidentType type,
                    Severity severity, Sector sector, LocalDate occurredOn) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.severity = severity;
        this.sector = sector;
        this.occurredOn = occurredOn;
        this.status = IncidentStatus.OPEN;
        this.registeredAt = LocalDateTime.now();
        this.cycle = 1;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public IncidentType getType() { return type; }
    public Severity getSeverity() { return severity; }
    public IncidentStatus getStatus() { return status; }
    public Sector getSector() { return sector; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public int getCycle() { return cycle; }
}
