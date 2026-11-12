package com.unip.api.history;

import com.unip.api.incident.Incident;
import com.unip.api.incident.IncidentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus newStatus;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @Column(length = 500)
    private String comment;

    protected StatusHistory() {}

    public StatusHistory(Incident incident, IncidentStatus previousStatus,
                         IncidentStatus newStatus, String comment) {
        this.incident = incident;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.comment = comment;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public IncidentStatus getPreviousStatus() { return previousStatus; }
    public IncidentStatus getNewStatus() { return newStatus; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public String getComment() { return comment; }
}