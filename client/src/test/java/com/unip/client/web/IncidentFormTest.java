package com.unip.client.web;

import com.unip.client.api.ApiClient;
import com.unip.client.api.ApiException;
import com.unip.client.api.ApiRequests;
import com.unip.client.dto.IncidentDto;
import com.unip.client.dto.SectorDto;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentFormTest {

    @Autowired MockMvc mvc;
    @MockitoBean ApiClient api;

    @BeforeEach
    void sectors() {
        when(api.sectors(anyString())).thenReturn(List.of(new SectorDto(1L, "Maintenance")));
    }

    private MockHttpSession employee() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUser.KEY,
                new SessionUser(1L, "Employee One", "employee1@demo.com", "EMPLOYEE", "Basic x"));
        return session;
    }

    private IncidentDto created() {
        return new IncidentDto(5L, "Dripping tap", "Drips", "WASTE", "LOW", "OPEN", "Maintenance",
                LocalDate.of(2026, 10, 28), LocalDateTime.of(2026, 10, 28, 10, 0), 1, null, null, "Employee One");
    }

    @Test
    void formPageLoadsWithSectors() throws Exception {
        mvc.perform(get("/incidents/new").session(employee()))
                .andExpect(status().isOk())
                .andExpect(view().name("incident-form"))
                .andExpect(content().string(containsString("Maintenance")));
    }

    @Test
    void validSubmissionCallsTheApiAndRedirectsToTheIncident() throws Exception {
        when(api.registerIncident(anyString(), any())).thenReturn(created());

        mvc.perform(post("/incidents").session(employee())
                        .param("title", "Dripping tap").param("description", "Drips")
                        .param("type", "WASTE").param("severity", "LOW")
                        .param("sectorId", "1").param("occurredOn", "2026-10-28"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"));

        verify(api).registerIncident("Basic x", new ApiRequests.Incident(
                "Dripping tap", "Drips", "WASTE", "LOW", 1L, LocalDate.of(2026, 10, 28)));
    }

    @Test
    void blankTitleStaysOnTheFormWithoutCallingTheApi() throws Exception {
        mvc.perform(post("/incidents").session(employee())
                        .param("title", "").param("description", "Drips")
                        .param("type", "WASTE").param("severity", "LOW")
                        .param("sectorId", "1").param("occurredOn", "2026-10-28"))
                .andExpect(status().isOk())
                .andExpect(view().name("incident-form"))
                .andExpect(model().attributeHasFieldErrors("form", "title"));

        verify(api, never()).registerIncident(anyString(), any());
    }

    @Test
    void apiRejectionIsShownOnTheForm() throws Exception {
        when(api.registerIncident(anyString(), any()))
                .thenThrow(new ApiException(404, "Sector not found"));

        mvc.perform(post("/incidents").session(employee())
                        .param("title", "Dripping tap").param("description", "Drips")
                        .param("type", "WASTE").param("severity", "LOW")
                        .param("sectorId", "99").param("occurredOn", "2026-10-28"))
                .andExpect(status().isOk())
                .andExpect(view().name("incident-form"))
                .andExpect(content().string(containsString("Sector not found")));
    }
}