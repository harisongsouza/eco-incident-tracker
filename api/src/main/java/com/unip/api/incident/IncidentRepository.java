package com.unip.api.incident;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface IncidentRepository
        extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    @Query("select i.status, count(i) from Incident i group by i.status")
    List<Object[]> countByStatus();

    @Query("select i.type, count(i) from Incident i group by i.type")
    List<Object[]> countByType();

    @Query("select i.sector.name, count(i) from Incident i group by i.sector.name order by i.sector.name")
    List<Object[]> countBySector();
}