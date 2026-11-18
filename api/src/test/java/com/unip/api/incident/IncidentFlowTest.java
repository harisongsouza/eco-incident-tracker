package com.unip.api.incident;

import org.springframework.security.test.context.support.WithUserDetails;
import com.jayway.jsonpath.JsonPath;
import com.unip.api.history.StatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithUserDetails("officer@demo.com")
@SpringBootTest
@AutoConfigureMockMvc
class IncidentFlowTest {

    @Autowired MockMvc mvc;
    @Autowired StatusHistoryRepository historyRepository;

    @Test
    void rootCauseRejectedWhileOpen() throws Exception {
        long id = registerIncident();
        mvc.perform(rootCause(id, "Worn gasket")).andExpect(status().isConflict());
    }

    @Test
    void rootCauseAcceptedUnderAnalysis() throws Exception {
        long id = registerAndAnalyze();
        mvc.perform(rootCause(id, "Worn gasket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootCause").value("Worn gasket"));
    }

    @Test
    void actionRejectedWhileOpen() throws Exception {
        long id = registerIncident();
        mvc.perform(action(id, LocalDate.now().plusDays(3))).andExpect(status().isConflict());
    }

    @Test
    void actionWithPastDeadlineIsBadRequest() throws Exception {
        long id = registerAndAnalyze();
        mvc.perform(action(id, LocalDate.now().minusDays(1))).andExpect(status().isBadRequest());
    }

    @Test
    void startExecutionWithoutRootCauseIsConflict() throws Exception {
        long id = registerAndAnalyze();
        mvc.perform(action(id, LocalDate.now().plusDays(3))).andExpect(status().isCreated());
        mvc.perform(post("/incidents/{id}/start-execution", id)).andExpect(status().isConflict());
    }

    @Test
    void startExecutionWithoutActionIsConflict() throws Exception {
        long id = registerAndAnalyze();
        mvc.perform(rootCause(id, "Worn gasket")).andExpect(status().isOk());
        mvc.perform(post("/incidents/{id}/start-execution", id)).andExpect(status().isConflict());
    }

    @Test
    void happyPathReachesInProgressAndWritesHistory() throws Exception {
        long id = registerAndAnalyze();
        mvc.perform(rootCause(id, "Worn gasket")).andExpect(status().isOk());
        mvc.perform(action(id, LocalDate.now().plusDays(3))).andExpect(status().isCreated());

        mvc.perform(post("/incidents/{id}/start-execution", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        assertEquals(2, historyRepository.findByIncidentIdOrderByChangedAtAsc(id).size());
    }

    @Test
    void actionRejectedAfterExecutionStarted() throws Exception {
        long id = registerAndAnalyze();
        mvc.perform(rootCause(id, "Worn gasket")).andExpect(status().isOk());
        mvc.perform(action(id, LocalDate.now().plusDays(3))).andExpect(status().isCreated());
        mvc.perform(post("/incidents/{id}/start-execution", id)).andExpect(status().isOk());

        mvc.perform(action(id, LocalDate.now().plusDays(5))).andExpect(status().isConflict());
    }

    // ---- helpers ----

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    rootCause(long id, String text) {
        return put("/incidents/{id}/root-cause", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rootCause\":\"" + text + "\"}");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
    action(long id, LocalDate deadline) {
        return post("/incidents/{id}/actions", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {"description":"Replace the tap gasket","responsibleId":2,
                 "deadline":"%s"}""".formatted(deadline));
    }

    private long registerAndAnalyze() throws Exception {
        long id = registerIncident();
        mvc.perform(post("/incidents/{id}/analyze", id)).andExpect(status().isOk());
        return id;
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