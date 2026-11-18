package com.unip.api.check;

import com.unip.api.incident.Incident;
import com.unip.api.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class EffectivenessCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Incident incident;

    @Column(nullable = false)
    private boolean effective;

    @Column(nullable = false, length = 1000)
    private String opinion;

    @Column(nullable = false)
    private int cycle;

    @Column(nullable = false)
    private LocalDateTime checkedAt;

    @ManyToOne(optional = false)
    private User checkedBy;

    protected EffectivenessCheck() {}

    public EffectivenessCheck(Incident incident, boolean effective, String opinion, User checkedBy) {
        this.incident = incident;
        this.effective = effective;
        this.opinion = opinion;
        this.cycle = incident.getCycle(); // the cycle being verified
        this.checkedAt = LocalDateTime.now();
        this.checkedBy = checkedBy;
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public boolean isEffective() { return effective; }
    public String getOpinion() { return opinion; }
    public int getCycle() { return cycle; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public User getCheckedBy() { return checkedBy; }
}