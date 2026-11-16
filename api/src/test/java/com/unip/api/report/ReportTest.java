package com.unip.api.report;

import com.jayway.jsonpath.JsonPath;
import com.unip.api.action.CorrectiveAction;
import com.unip.api.action.CorrectiveActionRepository;
import com.unip.api.incident.Incident;
import com.unip.api.incident.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReportTest {

    @Autowired MockMvc mvc;
    @Autowired IncidentRepository incidentRepository;
    @Autowired CorrectiveActionRepository actionRepository;

    @Test
    void summaryCountsNewIncident() throws Exception {
        long before = total();
        registerIncident();
        assertEquals(before + 1, total());

        mvc.perform(get("/reports/summary"))
                .andExpect(jsonPath("$.byStatus.OPEN").exists())
                .andExpect(jsonPath("$.byStatus.CLOSED").exists())
                .andExpect(jsonPath("$.byType.LEAK").exists());
    }

    @Test
    void overdueListsPendingActionsPastDeadline() throws Exception {
        long id = registerIncident();
        mvc.perform(post("/incidents/{id}/analyze", id)).andExpect(status().isOk());

        Incident incident = incidentRepository.findById(id).orElseThrow();
        actionRepository.save(new CorrectiveAction(
                incident, "Fix it", "John", LocalDate.now().minusDays(2)));

        mvc.perform(get("/reports/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.incidentId == %d)].daysOverdue", id).value(contains(2)));
    }

    @Test
    void actionsOfOpenIncidentWithFutureDeadlineAreNotOverdue() throws Exception {
        long id = registerIncident();
        mvc.perform(post("/incidents/{id}/analyze", id)).andExpect(status().isOk());
        mvc.perform(post("/incidents/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"description":"Fix it","responsible":"John","deadline":"%s"}"""
                                .formatted(LocalDate.now().plusDays(5))))
                .andExpect(status().isCreated());

        mvc.perform(get("/reports/overdue"))
                .andExpect(jsonPath("$[?(@.incidentId == %d)]", id).isEmpty());
    }

    private long total() throws Exception {
        String body = mvc.perform(get("/reports/summary"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.total")).longValue();
    }

    private long registerIncident() throws Exception {
        String json = """
            {"title":"Dripping tap","description":"Drips all day",
             "type":"LEAK","severity":"LOW","sectorId":1,
             "occurredOn":"2026-10-28"}""";

        String body = mvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}