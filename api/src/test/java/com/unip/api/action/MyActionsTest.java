package com.unip.api.action;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MyActionsTest {

    private static final String PASSWORD = "demo1234";
    private static final String EMPLOYEE1 = "employee1@demo.com";
    private static final String EMPLOYEE2 = "employee2@demo.com";
    private static final String OFFICER = "officer@demo.com";

    @Autowired MockMvc mvc;

    @Test
    void anonymousUserIsUnauthorized() throws Exception {
        mvc.perform(get("/corrective-actions/mine")).andExpect(status().isUnauthorized());
    }

    @Test
    void assignedEmployeeSeesHisPendingActionAndOthersDoNot() throws Exception {
        long incident = registerAndAnalyze();
        createAction(incident, idOf(EMPLOYEE2), "Replace gasket", 3);

        mvc.perform(get("/corrective-actions/mine").with(as(EMPLOYEE2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.incidentId == %d)].description", incident)
                        .value(contains("Replace gasket")));

        mvc.perform(get("/corrective-actions/mine").with(as(EMPLOYEE1)))
                .andExpect(jsonPath("$[?(@.incidentId == %d)]", incident).isEmpty());
    }

    @Test
    void completedActionLeavesTheList() throws Exception {
        long incident = registerAndAnalyze();
        long action = createAction(incident, idOf(EMPLOYEE2), "Replace gasket", 3);
        mvc.perform(post("/incidents/{id}/start-execution", incident).with(as(OFFICER)))
                .andExpect(status().isOk());

        mvc.perform(post("/corrective-actions/{id}/complete", action).with(as(EMPLOYEE2))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Done\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/corrective-actions/mine").with(as(EMPLOYEE2)))
                .andExpect(jsonPath("$[?(@.incidentId == %d)]", incident).isEmpty());
    }

    @Test
    void actionOfADismissedIncidentIsNotListed() throws Exception {
        long incident = registerAndAnalyze();
        createAction(incident, idOf(EMPLOYEE2), "Replace gasket", 3);

        mvc.perform(post("/incidents/{id}/dismiss", incident).with(as(OFFICER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"justification\":\"Duplicate\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/corrective-actions/mine").with(as(EMPLOYEE2)))
                .andExpect(jsonPath("$[?(@.incidentId == %d)]", incident).isEmpty());
    }

    @Test
    void earliestDeadlineComesFirst() throws Exception {
        long incident = registerAndAnalyze();
        long user = idOf(EMPLOYEE2);
        createAction(incident, user, "Later one", 6);
        createAction(incident, user, "Sooner one", 2);

        mvc.perform(get("/corrective-actions/mine").with(as(EMPLOYEE2)))
                .andExpect(jsonPath("$[?(@.incidentId == %d)].description", incident)
                        .value(contains("Sooner one", "Later one")));
    }

    // ---- helpers ----

    private RequestPostProcessor as(String email) {
        return httpBasic(email, PASSWORD);
    }

    private long idOf(String email) throws Exception {
        String body = mvc.perform(get("/users/me").with(as(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** employee1 registers, the officer analyzes and registers the root cause. */
    private long registerAndAnalyze() throws Exception {
        String json = """
            {"title":"Dripping tap","description":"Drips all day",
             "type":"WASTE","severity":"LOW","sectorId":1,
             "occurredOn":"2026-10-28"}""";
        String body = mvc.perform(post("/incidents").with(as(EMPLOYEE1))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        mvc.perform(post("/incidents/{id}/analyze", id).with(as(OFFICER))).andExpect(status().isOk());
        mvc.perform(put("/incidents/{id}/root-cause", id).with(as(OFFICER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rootCause\":\"Worn gasket\"}"))
                .andExpect(status().isOk());
        return id;
    }

    private long createAction(long incidentId, long responsibleId, String description, int days)
            throws Exception {
        String json = """
            {"description":"%s","responsibleId":%d,"deadline":"%s"}"""
                .formatted(description, responsibleId, LocalDate.now().plusDays(days));
        String body = mvc.perform(post("/incidents/{id}/actions", incidentId).with(as(OFFICER))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}