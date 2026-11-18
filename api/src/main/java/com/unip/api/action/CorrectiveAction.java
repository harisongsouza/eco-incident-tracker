package com.unip.api.action;

import com.unip.api.incident.Incident;
import com.unip.api.shared.exception.BusinessRuleException;
import com.unip.api.user.User;
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

    @ManyToOne(optional = false)
    private User responsible;

    @Column(nullable = false)
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;

    @Column(nullable = false)
    private int cycle;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 500)
    private String completionNote;

    private LocalDateTime completedAt;

    protected CorrectiveAction() {}

    public CorrectiveAction(Incident incident, String description,
                            User responsible, LocalDate deadline) {
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
    public User getResponsible() { return responsible; }
    public LocalDate getDeadline() { return deadline; }
    public ActionStatus getStatus() { return status; }
    public int getCycle() { return cycle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCompletionNote() { return completionNote; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public void complete(String note) {
        if (status == ActionStatus.COMPLETED) {
            throw new BusinessRuleException("Action is already completed");
        }
        this.status = ActionStatus.COMPLETED;
        this.completionNote = note;
        this.completedAt = LocalDateTime.now();
    }
}