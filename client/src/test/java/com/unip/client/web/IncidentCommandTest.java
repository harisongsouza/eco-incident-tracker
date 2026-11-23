package com.unip.client.web;

import com.unip.client.api.ApiClient;
import com.unip.client.api.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentCommandTest {

    @Autowired MockMvc mvc;
    @MockitoBean ApiClient api;

    private MockHttpSession officer() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUser.KEY, new SessionUser(
                3L, "Environmental Officer", "officer@demo.com", "ENVIRONMENTAL_OFFICER", "Basic x"));
        return session;
    }

    @Test
    void analyzeRedirectsBackWithSuccessMessage() throws Exception {
        mvc.perform(post("/incidents/1/analyze").session(officer()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/1"))
                .andExpect(flash().attribute("success", "Analysis started"));

        verify(api).analyze("Basic x", 1L);
    }

    @Test
    void apiConflictBecomesAnErrorMessageOnTheDetailPage() throws Exception {
        when(api.analyze("Basic x", 1L))
                .thenThrow(new ApiException(409, "Cannot move incident from CLOSED to UNDER_ANALYSIS"));

        mvc.perform(post("/incidents/1/analyze").session(officer()))
                .andExpect(redirectedUrl("/incidents/1"))
                .andExpect(flash().attribute("error", "Cannot move incident from CLOSED to UNDER_ANALYSIS"));
    }

    @Test
    void expiredCredentialsSendTheUserToLogin() throws Exception {
        when(api.analyze("Basic x", 1L)).thenThrow(new ApiException(401, "Unauthorized"));

        mvc.perform(post("/incidents/1/analyze").session(officer()))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void completingAnActionReturnsToItsIncident() throws Exception {
        mvc.perform(post("/corrective-actions/7/complete").session(officer())
                        .param("incidentId", "1").param("note", " Done "))
                .andExpect(redirectedUrl("/incidents/1"))
                .andExpect(flash().attribute("success", "Action completed"));

        verify(api).completeAction("Basic x", 7L, "Done"); // trimmed
    }

    @Test
    void ineffectiveVerificationHasItsOwnMessage() throws Exception {
        mvc.perform(post("/incidents/1/checks").session(officer())
                        .param("effective", "false").param("opinion", "Still dripping"))
                .andExpect(flash().attribute("success", "Verified as ineffective: a new cycle was started"));

        verify(api).verify("Basic x", 1L, false, "Still dripping");
    }

    @Test
    void anonymousUserCannotRunCommands() throws Exception {
        mvc.perform(post("/incidents/1/analyze"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void completingFromMyActionsReturnsToMyActions() throws Exception {
        mvc.perform(post("/corrective-actions/7/complete").session(officer())
                        .param("incidentId", "1").param("note", "Done").param("returnTo", "my-actions"))
                .andExpect(redirectedUrl("/my-actions"))
                .andExpect(flash().attribute("success", "Action completed"));

        verify(api).completeAction("Basic x", 7L, "Done");
    }
}