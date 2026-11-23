package com.unip.client.web;

import com.unip.client.api.ApiClient;
import com.unip.client.dto.ActionDto;
import com.unip.client.dto.IncidentDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentButtonsTest {

    @Autowired MockMvc mvc;
    @MockitoBean ApiClient api;

    private MockHttpSession session(long id, String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUser.KEY,
                new SessionUser(id, "User " + id, "u" + id + "@demo.com", role, "Basic x"));
        return session;
    }

    private void incidentInStatus(String status, String rootCause, List<ActionDto> actions) {
        when(api.incident(anyString(), org.mockito.ArgumentMatchers.eq(1L))).thenReturn(new IncidentDto(
                1L, "Dripping tap", "Drips", "WASTE", "LOW", status, "Maintenance",
                LocalDate.of(2026, 10, 28), LocalDateTime.of(2026, 10, 28, 10, 0),
                1, rootCause, null, "Employee One"));
        when(api.actions(anyString(), org.mockito.ArgumentMatchers.eq(1L))).thenReturn(actions);
    }

    private ActionDto pendingActionFor(long responsibleId) {
        return new ActionDto(7L, 1L, "Replace the gasket", responsibleId, "Responsible",
                LocalDate.now().plusDays(3), "PENDING", 1, null, null);
    }

    @Test
    void officerSeesStartAnalysisOnAnOpenIncident() throws Exception {
        incidentInStatus("OPEN", null, List.of());

        mvc.perform(get("/incidents/1").session(session(3, "ENVIRONMENTAL_OFFICER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/incidents/1/analyze")))
                .andExpect(content().string(containsString("/incidents/1/dismiss")));
    }

    @Test
    void employeeNeverSeesTheOfficerPanel() throws Exception {
        incidentInStatus("OPEN", null, List.of());

        mvc.perform(get("/incidents/1").session(session(1, "EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/incidents/1/analyze"))))
                .andExpect(content().string(not(containsString("Next step"))));
    }

    @Test
    void underAnalysisShowsRootCauseAndActionForms() throws Exception {
        incidentInStatus("UNDER_ANALYSIS", null, List.of());

        mvc.perform(get("/incidents/1").session(session(3, "ENVIRONMENTAL_OFFICER")))
                .andExpect(content().string(containsString("/incidents/1/root-cause")))
                .andExpect(content().string(containsString("/incidents/1/actions")))
                .andExpect(content().string(containsString("Requires a root cause")));
    }

    @Test
    void onlyTheResponsibleEmployeeSeesTheCompleteButton() throws Exception {
        incidentInStatus("IN_PROGRESS", "Worn gasket", List.of(pendingActionFor(2)));

        mvc.perform(get("/incidents/1").session(session(2, "EMPLOYEE")))
                .andExpect(content().string(containsString("/corrective-actions/7/complete")));

        mvc.perform(get("/incidents/1").session(session(1, "EMPLOYEE")))
                .andExpect(content().string(not(containsString("/corrective-actions/7/complete"))));
    }

    @Test
    void officerSeesVerificationFormWhenAwaitingVerification() throws Exception {
        incidentInStatus("AWAITING_VERIFICATION", "Worn gasket", List.of());

        mvc.perform(get("/incidents/1").session(session(3, "ENVIRONMENTAL_OFFICER")))
                .andExpect(content().string(containsString("/incidents/1/checks")));
    }

    @Test
    void closedIncidentHasNoActionButtons() throws Exception {
        incidentInStatus("CLOSED", "Worn gasket", List.of());

        mvc.perform(get("/incidents/1").session(session(3, "ENVIRONMENTAL_OFFICER")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Next step"))));
    }
}