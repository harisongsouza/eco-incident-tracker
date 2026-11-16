package com.unip.api.incident;

import com.jayway.jsonpath.JsonPath;
import com.unip.api.history.StatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentCycleTest {

    @Autowired MockMvc mvc;
    @Autowired StatusHistoryRepository historyRepository;

    @Test
    void lastCompletedActionMovesIncidentToAwaitingVerification() throws Exception {
        long id = analyzedWithRootCause();
        long a1 = createAction(id);
        long a2 = createAction(id);
        mvc.perform(post("/incidents/{id}/start-execution", id)).andExpect(status().isOk());

        mvc.perform(complete(a1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        assertStatus(id, "IN_PROGRESS"); // one action is still pending

        mvc.perform(complete(a2)).andExpect(status().isOk());
        assertStatus(id, "AWAITING_VERIFICATION");
    }

    @Test
    void effectiveCheckClosesIncident() throws Exception {
        long id = inVerification();

        mvc.perform(check(id, true, "Tap no longer drips"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentStatus").value("CLOSED"));

        mvc.perform(get("/incidents/{id}", id))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
    }

    @Test
    void ineffectiveCheckReopensWithNewCycleAndClearsRootCause() throws Exception {
        long id = inVerification();

        mvc.perform(check(id, false, "Still dripping"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cycle").value(1));

        mvc.perform(get("/incidents/{id}", id))
                .andExpect(jsonPath("$.status").value("UNDER_ANALYSIS"))
                .andExpect(jsonPath("$.cycle").value(2))
                .andExpect(jsonPath("$.rootCause").isEmpty());
    }

    @Test
    void secondCycleNeedsNewRootCauseAndNewActionThenCloses() throws Exception {
        long id = inVerification();
        mvc.perform(check(id, false, "Still dripping")).andExpect(status().isCreated());

        // no root cause yet
        mvc.perform(post("/incidents/{id}/start-execution", id)).andExpect(status().isConflict());

        mvc.perform(rootCause(id, "Pipe corrosion")).andExpect(status().isOk());
        // root cause ok, but no action in cycle 2
        mvc.perform(post("/incidents/{id}/start-execution", id)).andExpect(status().isConflict());

        long action = createAction(id);
        mvc.perform(post("/incidents/{id}/start-execution", id)).andExpect(status().isOk());
        mvc.perform(complete(action)).andExpect(status().isOk());
        mvc.perform(check(id, true, "Fixed")).andExpect(status().isCreated());

        assertStatus(id, "CLOSED");
        assertEquals(7, historyRepository.findByIncidentIdOrderByChangedAtAsc(id).size());
    }

    @Test
    void completingActionTwiceIsConflict() throws Exception {
        long id = analyzedWithRootCause();
        long action = createAction(id);
        mvc.perform(post("/incidents/{id}/start-execution", id)).andExpect(status().isOk());
        mvc.perform(complete(action)).andExpect(status().isOk());

        mvc.perform(complete(action)).andExpect(status().isConflict());
    }

    @Test
    void completingActionWhileUnderAnalysisIsConflict() throws Exception {
        long id = analyzedWithRootCause();
        long action = createAction(id);

        mvc.perform(complete(action)).andExpect(status().isConflict());
    }

    @Test
    void completingUnknownActionIsNotFound() throws Exception {
        mvc.perform(complete(999999)).andExpect(status().isNotFound());
    }

    @Test
    void checkWhenNotAwaitingVerificationIsConflict() throws Exception {
        long id = analyzedWithRootCause();
        mvc.perform(check(id, true, "x")).andExpect(status().isConflict());
    }

    @Test
    void checkWithoutEffectiveFieldIsBadRequest() throws Exception {
        long id = inVerification();
        mvc.perform(post("/incidents/{id}/checks", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opinion\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void closedIncidentIsImmutable() throws Exception {
        long id = inVerification();
        mvc.perform(check(id, true, "Fixed")).andExpect(status().isCreated());

        mvc.perform(rootCause(id, "x")).andExpect(status().isConflict());
        mvc.perform(action(id)).andExpect(status().isConflict());
        mvc.perform(post("/incidents/{id}/analyze", id)).andExpect(status().isConflict());
        mvc.perform(dismiss(id, "x")).andExpect(status().isConflict());
    }

    @Test
    void openIncidentCanBeDismissed() throws Exception {
        long id = registerIncident();
        mvc.perform(dismiss(id, "Not an environmental issue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));
    }

    @Test
    void dismissWithoutJustificationIsBadRequest() throws Exception {
        long id = registerIncident();
        mvc.perform(dismiss(id, "")).andExpect(status().isBadRequest());
    }

    @Test
    void incidentInProgressCannotBeDismissed() throws Exception {
        long id = analyzedWithRootCause();
        createAction(id);
        mvc.perform(post("/incidents/{id}/start-execution", id)).andExpect(status().isOk());

        mvc.perform(dismiss(id, "x")).andExpect(status().isConflict());
    }

    // ---- scenarios ----

    private long analyzedWithRootCause() throws Exception {
        long id = registerIncident();
        mvc.perform(post("/incidents/{id}/analyze", id)).andExpect(status().isOk());
        mvc.perform(rootCause(id, "Worn gasket")).andExpect(status().isOk());
        return id;
    }

    private long inVerification() throws Exception {
        long id = analyzedWithRootCause();
        long action = createAction(id);
        mvc.perform(post("/incidents/{id}/start-execution", id)).andExpect(status().isOk());
        mvc.perform(complete(action)).andExpect(status().isOk());
        return id;
    }

    // ---- requests ----

    private MockHttpServletRequestBuilder rootCause(long id, String text) {
        return put("/incidents/{id}/root-cause", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rootCause\":\"" + text + "\"}");
    }

    private MockHttpServletRequestBuilder action(long incidentId) {
        return post("/incidents/{id}/actions", incidentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {"description":"Replace the tap gasket","responsible":"John",
                 "deadline":"%s"}""".formatted(LocalDate.now().plusDays(3)));
    }

    private MockHttpServletRequestBuilder complete(long actionId) {
        return post("/corrective-actions/{id}/complete", actionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Done\"}");
    }

    private MockHttpServletRequestBuilder check(long id, boolean effective, String opinion) {
        return post("/incidents/{id}/checks", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"effective\":" + effective + ",\"opinion\":\"" + opinion + "\"}");
    }

    private MockHttpServletRequestBuilder dismiss(long id, String justification) {
        return post("/incidents/{id}/dismiss", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"justification\":\"" + justification + "\"}");
    }

    // ---- helpers ----

    private long createAction(long incidentId) throws Exception {
        String body = mvc.perform(action(incidentId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void assertStatus(long id, String expected) throws Exception {
        mvc.perform(get("/incidents/{id}", id))
                .andExpect(jsonPath("$.status").value(expected));
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