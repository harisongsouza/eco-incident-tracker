package com.unip.client.web;

import com.unip.client.api.ApiClient;
import com.unip.client.api.ApiException;
import com.unip.client.dto.*;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentPagesTest {

    @Autowired MockMvc mvc;
    @MockitoBean ApiClient api;

    private MockHttpSession loggedIn() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUser.KEY, new SessionUser(
                3L, "Environmental Officer", "officer@demo.com", "ENVIRONMENTAL_OFFICER", "Basic x"));
        return session;
    }

    private IncidentDto incident() {
        return new IncidentDto(1L, "Dripping tap", "Drips all day", "WASTE", "LOW", "OPEN",
                "Maintenance", LocalDate.of(2026, 10, 28), LocalDateTime.of(2026, 10, 28, 10, 0),
                1, null, null, "Employee One");
    }

    @Test
    void listShowsIncidentsFromTheApi() throws Exception {
        when(api.sectors(anyString())).thenReturn(List.of(new SectorDto(1L, "Maintenance")));
        when(api.searchIncidents(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(incident()));

        mvc.perform(get("/incidents").session(loggedIn()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dripping tap")));
    }

    @Test
    void listForwardsTheFiltersToTheApi() throws Exception {
        when(api.sectors(anyString())).thenReturn(List.of());
        when(api.searchIncidents(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mvc.perform(get("/incidents").param("status", "OPEN").param("sectorId", "1")
                        .session(loggedIn()))
                .andExpect(status().isOk());

        verify(api).searchIncidents(eq("Basic x"), eq("OPEN"), isNull(), eq(1L), isNull(), isNull());
    }

    @Test
    void invalidPeriodIsShownInline() throws Exception {
        when(api.sectors(anyString())).thenReturn(List.of());
        when(api.searchIncidents(anyString(), any(), any(), any(), any(), any()))
                .thenThrow(new ApiException(400, "Invalid period"));

        mvc.perform(get("/incidents").session(loggedIn()))
                .andExpect(status().isOk())
                .andExpect(view().name("incidents"))
                .andExpect(content().string(containsString("Invalid period")));
    }

    @Test
    void detailShowsActionsHistoryAndOverdueBadge() throws Exception {
        when(api.incident(anyString(), eq(1L))).thenReturn(incident());
        when(api.actions(anyString(), eq(1L))).thenReturn(List.of(new ActionDto(
                1L, 1L, "Replace the tap gasket", 2L, "Employee Two",
                LocalDate.now().minusDays(2), "PENDING", 1, null, null)));
        when(api.checks(anyString(), eq(1L))).thenReturn(List.of());
        when(api.history(anyString(), eq(1L))).thenReturn(List.of(new HistoryDto(
                "OPEN", "UNDER_ANALYSIS", LocalDateTime.of(2026, 10, 28, 11, 0),
                "Analysis started", "Environmental Officer")));

        mvc.perform(get("/incidents/1").session(loggedIn()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Replace the tap gasket")))
                .andExpect(content().string(containsString("Analysis started")))
                .andExpect(content().string(containsString("Overdue")));
    }

    @Test
    void invisibleIncidentShowsTheApiMessage() throws Exception {
        when(api.incident(anyString(), eq(99L)))
                .thenThrow(new ApiException(404, "Incident not found"));

        mvc.perform(get("/incidents/99").session(loggedIn()))
                .andExpect(view().name("api-error.html"))
                .andExpect(content().string(containsString("Incident not found")));
    }

    @Test
    void unauthorizedFromTheApiSendsTheUserToLogin() throws Exception {
        when(api.incident(anyString(), eq(1L))).thenThrow(new ApiException(401, "Unauthorized"));

        mvc.perform(get("/incidents/1").session(loggedIn()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}