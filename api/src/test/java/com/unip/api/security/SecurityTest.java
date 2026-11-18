package com.unip.api.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    private static final String PASSWORD = "demo1234";
    private static final String EMPLOYEE1 = "employee1@demo.com";
    private static final String EMPLOYEE2 = "employee2@demo.com";
    private static final String OFFICER = "officer@demo.com";
    private static final String MANAGER = "manager@demo.com";

    @Autowired MockMvc mvc;

    @Test
    void requestWithoutCredentialsIsUnauthorized() throws Exception {
        mvc.perform(get("/incidents")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(get("/incidents").with(httpBasic(OFFICER, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeCannotAnalyzeButOfficerCan() throws Exception {
        long id = register(EMPLOYEE1);

        mvc.perform(post("/incidents/{id}/analyze", id).with(as(EMPLOYEE1)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/incidents/{id}/analyze", id).with(as(OFFICER)))
                .andExpect(status().isOk());
    }

    @Test
    void managerCannotDriveTheWorkflow() throws Exception {
        long id = register(EMPLOYEE1);
        mvc.perform(post("/incidents/{id}/analyze", id).with(as(MANAGER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeSeesOnlyOwnIncidents() throws Exception {
        long id = register(EMPLOYEE1);

        mvc.perform(get("/incidents/{id}", id).with(as(EMPLOYEE1))).andExpect(status().isOk());
        mvc.perform(get("/incidents/{id}", id).with(as(EMPLOYEE2))).andExpect(status().isNotFound());
        mvc.perform(get("/incidents/{id}/history", id).with(as(EMPLOYEE2))).andExpect(status().isNotFound());
        mvc.perform(get("/incidents/{id}/actions", id).with(as(EMPLOYEE2))).andExpect(status().isNotFound());

        mvc.perform(get("/incidents").with(as(EMPLOYEE2)))
                .andExpect(jsonPath("$[?(@.id == %d)]", id).isEmpty());
        mvc.perform(get("/incidents").with(as(EMPLOYEE1)))
                .andExpect(jsonPath("$[?(@.id == %d)]", id).isNotEmpty());
        mvc.perform(get("/incidents").with(as(OFFICER)))
                .andExpect(jsonPath("$[?(@.id == %d)]", id).isNotEmpty());
    }

    @Test
    void employeeAssignedAnActionCanSeeTheIncident() throws Exception {
        long id = register(EMPLOYEE1);
        prepareExecution(id, idOf(EMPLOYEE2));

        mvc.perform(get("/incidents/{id}", id).with(as(EMPLOYEE2))).andExpect(status().isOk());
        mvc.perform(get("/incidents").with(as(EMPLOYEE2)))
                .andExpect(jsonPath("$[?(@.id == %d)]", id).isNotEmpty());
    }

    @Test
    void onlyTheResponsibleOrAnOfficerCanCompleteAnAction() throws Exception {
        long id = register(EMPLOYEE1);
        long action = prepareExecution(id, idOf(EMPLOYEE2));

        mvc.perform(complete(action).with(as(EMPLOYEE1))).andExpect(status().isForbidden());
        mvc.perform(complete(action).with(as(MANAGER))).andExpect(status().isForbidden());
        mvc.perform(complete(action).with(as(EMPLOYEE2))).andExpect(status().isOk());
    }

    @Test
    void reportsAreRestrictedToOfficerAndManager() throws Exception {
        mvc.perform(get("/reports/summary").with(as(EMPLOYEE1))).andExpect(status().isForbidden());
        mvc.perform(get("/reports/summary").with(as(MANAGER))).andExpect(status().isOk());
        mvc.perform(get("/reports/summary").with(as(OFFICER))).andExpect(status().isOk());
    }

    @Test
    void historyRecordsWhoMadeTheChange() throws Exception {
        long id = register(EMPLOYEE1);
        mvc.perform(post("/incidents/{id}/analyze", id).with(as(OFFICER))).andExpect(status().isOk());

        mvc.perform(get("/incidents/{id}/history", id).with(as(OFFICER)))
                .andExpect(jsonPath("$[0].changedBy").value("Environmental Officer"));
    }

    @Test
    void actionWithUnknownResponsibleIsNotFound() throws Exception {
        long id = register(EMPLOYEE1);
        mvc.perform(post("/incidents/{id}/analyze", id).with(as(OFFICER))).andExpect(status().isOk());

        mvc.perform(post("/incidents/{id}/actions", id).with(as(OFFICER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionJson(999999)))
                .andExpect(status().isNotFound());
    }

    @Test
    void managerCreatesUserWhoCanThenLogInAndDuplicateEmailIsConflict() throws Exception {
        String email = "new" + System.nanoTime() + "@demo.com";
        String json = """
            {"name":"New Employee","email":"%s","password":"secret123","role":"EMPLOYEE"}"""
                .formatted(email);

        mvc.perform(post("/users").with(as(MANAGER))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mvc.perform(get("/users/me").with(httpBasic(email, "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        mvc.perform(post("/users").with(as(MANAGER))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void employeeCannotCreateUsers() throws Exception {
        String json = """
            {"name":"X Person","email":"x%d@demo.com","password":"secret123","role":"MANAGER"}"""
                .formatted(System.nanoTime());

        mvc.perform(post("/users").with(as(EMPLOYEE1))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());
    }

    // ---- helpers ----

    private RequestPostProcessor as(String email) {
        return httpBasic(email, PASSWORD);
    }

    private long register(String asEmail) throws Exception {
        String json = """
            {"title":"Dripping tap","description":"Drips all day",
             "type":"WASTE","severity":"LOW","sectorId":1,
             "occurredOn":"2026-10-28"}""";

        String body = mvc.perform(post("/incidents").with(as(asEmail))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long idOf(String email) throws Exception {
        String body = mvc.perform(get("/users/me").with(as(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** analyze, root cause, action for the given user, start execution. Returns the action id. */
    private long prepareExecution(long incidentId, long responsibleId) throws Exception {
        mvc.perform(post("/incidents/{id}/analyze", incidentId).with(as(OFFICER)))
                .andExpect(status().isOk());
        mvc.perform(put("/incidents/{id}/root-cause", incidentId).with(as(OFFICER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rootCause\":\"Worn gasket\"}"))
                .andExpect(status().isOk());

        String body = mvc.perform(post("/incidents/{id}/actions", incidentId).with(as(OFFICER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionJson(responsibleId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mvc.perform(post("/incidents/{id}/start-execution", incidentId).with(as(OFFICER)))
                .andExpect(status().isOk());
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String actionJson(long responsibleId) {
        return """
            {"description":"Replace the tap gasket","responsibleId":%d,
             "deadline":"%s"}""".formatted(responsibleId, LocalDate.now().plusDays(3));
    }

    private MockHttpServletRequestBuilder complete(long actionId) {
        return post("/corrective-actions/{id}/complete", actionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Done\"}");
    }
}