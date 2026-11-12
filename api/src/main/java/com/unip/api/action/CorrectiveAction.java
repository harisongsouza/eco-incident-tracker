package com.unip.api.action;

import com.unip.api.incident.Incident;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class CorrectiveAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Incident incident;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private String responsible;

    @Column(nullable = false)
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;

    @Column(nullable = false)
    private int cycle;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected CorrectiveAction() {}

    public CorrectiveAction(Incident incident, String description,
                            String responsible, LocalDate deadline) {
        this.incident = incident;
        this.description = description;
        this.responsible = responsible;
        this.deadline = deadline;
        this.status = ActionStatus.PENDING;
        this.cycle = incident.getCycle(); // the action belongs to the current cycle
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public String getDescription() { return description; }
    public String getResponsible() { return responsible; }
    public LocalDate getDeadline() { return deadline; }
    public ActionStatus getStatus() { return status; }
    public int getCycle() { return cycle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}