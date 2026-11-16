package com.unip.api.check;

import com.unip.api.incident.Incident;
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

    protected EffectivenessCheck() {}

    public EffectivenessCheck(Incident incident, boolean effective, String opinion) {
        this.incident = incident;
        this.effective = effective;
        this.opinion = opinion;
        this.cycle = incident.getCycle(); // the cycle being verified
        this.checkedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public boolean isEffective() { return effective; }
    public String getOpinion() { return opinion; }
    public int getCycle() { return cycle; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
}