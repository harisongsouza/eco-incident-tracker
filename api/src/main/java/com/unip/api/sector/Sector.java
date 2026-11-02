package com.unip.api.sector;

import jakarta.persistence.*;

@Entity
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    protected Sector() {}

    public Long getId() { return id; }
    public String getName() { return name; }
}
