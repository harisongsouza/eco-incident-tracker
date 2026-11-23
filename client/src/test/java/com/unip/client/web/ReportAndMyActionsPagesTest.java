package com.unip.client.web;

import com.unip.client.api.ApiClient;
import com.unip.client.api.ApiException;
import com.unip.client.dto.MyActionDto;
import com.unip.client.dto.OverdueDto;
import com.unip.client.dto.SummaryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReportAndMyActionsPagesTest {

    @Autowired MockMvc mvc;
    @MockitoBean ApiClient api;

    private MockHttpSession session(long id, String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUser.KEY,
                new SessionUser(id, "User " + id, "u" + id + "@demo.com", role, "Basic x"));
        return session;
    }

    private MyActionDto action(String incidentStatus, LocalDate deadline) {
        return new MyActionDto(7L, 1L, "Dripping tap", incidentStatus,
                "Replace the gasket", deadline, 1);
    }

    // ---- reports ----

    @Test
    void officerSeesSummaryAndOverdueActions() throws Exception {
        when(api.summary(anyString())).thenReturn(new SummaryDto(4,
                Map.of("OPEN", 3L, "CLOSED", 1L), Map.of("LEAK", 4L), Map.of("Maintenance", 4L)));
        when(api.overdue(anyString())).thenReturn(List.of(new OverdueDto(
                7L, 1L, "Dripping tap", "Replace the gasket", "Employee Two",
                LocalDate.now().minusDays(3), 3)));

        mvc.perform(get("/reports").session(session(3, "ENVIRONMENTAL_OFFICER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Maintenance")))
                .andExpect(content().string(containsString("Replace the gasket")))
                .andExpect(content().string(containsString("Employee Two")));
    }

    @Test
    void emptyOverdueListShowsAFriendlyMessage() throws Exception {
        when(api.summary(anyString())).thenReturn(new SummaryDto(0, Map.of(), Map.of(), Map.of()));
        when(api.overdue(anyString())).thenReturn(List.of());

        mvc.perform(get("/reports").session(session(4, "MANAGER")))
                .andExpect(content().string(containsString("No overdue actions")));
    }

    @Test
    void employeeIsRefusedByTheApiAndSeesTheMessage() throws Exception {
        when(api.summary(anyString()))
                .thenThrow(new ApiException(403, "You do not have permission for this operation"));

        mvc.perform(get("/reports").session(session(1, "EMPLOYEE")))
                .andExpect(view().name("api-error.html"))
                .andExpect(content().string(containsString("You do not have permission")));
    }

    @Test
    void reportsLinkIsHiddenFromEmployeesAndShownToTheOfficer() throws Exception {
        when(api.myActions(anyString())).thenReturn(List.of());

        mvc.perform(get("/my-actions").session(session(1, "EMPLOYEE")))
                .andExpect(content().string(not(containsString("/reports"))));
        mvc.perform(get("/my-actions").session(session(3, "ENVIRONMENTAL_OFFICER")))
                .andExpect(content().string(containsString("/reports")));
    }

    // ---- my actions ----

    @Test
    void inProgressActionShowsTheCompleteForm() throws Exception {
        when(api.myActions(anyString()))
                .thenReturn(List.of(action("IN_PROGRESS", LocalDate.now().plusDays(3))));

        mvc.perform(get("/my-actions").session(session(2, "EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/corrective-actions/7/complete")))
                .andExpect(content().string(containsString("my-actions"))); // returnTo field
    }

    @Test
    void actionOfAnIncidentStillUnderAnalysisCannotBeCompletedYet() throws Exception {
        when(api.myActions(anyString()))
                .thenReturn(List.of(action("UNDER_ANALYSIS", LocalDate.now().plusDays(3))));

        mvc.perform(get("/my-actions").session(session(2, "EMPLOYEE")))
                .andExpect(content().string(not(containsString("/corrective-actions/7/complete"))))
                .andExpect(content().string(containsString("Waiting for the officer")));
    }

    @Test
    void overdueActionIsHighlighted() throws Exception {
        when(api.myActions(anyString()))
                .thenReturn(List.of(action("IN_PROGRESS", LocalDate.now().minusDays(2))));

        mvc.perform(get("/my-actions").session(session(2, "EMPLOYEE")))
                .andExpect(content().string(containsString("Overdue")));
    }

    @Test
    void emptyListShowsAFriendlyMessage() throws Exception {
        when(api.myActions(anyString())).thenReturn(List.of());

        mvc.perform(get("/my-actions").session(session(2, "EMPLOYEE")))
                .andExpect(content().string(containsString("no pending actions")));
    }
}