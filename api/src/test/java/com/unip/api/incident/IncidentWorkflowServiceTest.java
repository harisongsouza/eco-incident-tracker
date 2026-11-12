package com.unip.api.incident;

import com.jayway.jsonpath.JsonPath;
import com.unip.api.history.StatusHistory;
import com.unip.api.history.StatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentWorkflowServiceTest {

    @Autowired MockMvc mvc;
    @Autowired StatusHistoryRepository historyRepository;

    @Test
    void analyzeMovesOpenIncidentToUnderAnalysis() throws Exception {
        long id = registerIncident();

        mvc.perform(post("/incidents/{id}/analyze", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_ANALYSIS"));
    }

    @Test
    void analyzingTwiceReturnsConflict() throws Exception {
        long id = registerIncident();

        mvc.perform(post("/incidents/{id}/analyze", id)).andExpect(status().isOk());
        mvc.perform(post("/incidents/{id}/analyze", id)).andExpect(status().isConflict());
    }

    @Test
    void analyzingUnknownIncidentReturnsNotFound() throws Exception {
        mvc.perform(post("/incidents/{id}/analyze", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void analyzeWritesStatusHistory() throws Exception {
        long id = registerIncident();

        mvc.perform(post("/incidents/{id}/analyze", id)).andExpect(status().isOk());

        List<StatusHistory> entries =
                historyRepository.findByIncidentIdOrderByChangedAtAsc(id);
        assertEquals(1, entries.size());
        assertEquals(IncidentStatus.OPEN, entries.get(0).getPreviousStatus());
        assertEquals(IncidentStatus.UNDER_ANALYSIS, entries.get(0).getNewStatus());
    }

    private long registerIncident() throws Exception {
        String json = """
            {"title":"Dripping tap","description":"Drips all day",
             "type":"WASTE","severity":"LOW","sectorId":1,
             "occurredOn":"2026-10-28"}""";

        String body = mvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}