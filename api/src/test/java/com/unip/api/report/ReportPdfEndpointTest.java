package com.unip.api.report;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReportPdfEndpointTest {

    private static final String PASSWORD = "demo1234";

    @Autowired MockMvc mvc;

    @Test
    void officerDownloadsAPdf() throws Exception {
        byte[] body = mvc.perform(get("/reports/pdf").with(httpBasic("officer@demo.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("incident-report.pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        assertEquals("%PDF", new String(body, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void managerDownloadsAPdf() throws Exception {
        mvc.perform(get("/reports/pdf").with(httpBasic("manager@demo.com", PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void employeeIsForbidden() throws Exception {
        mvc.perform(get("/reports/pdf").with(httpBasic("employee1@demo.com", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserIsUnauthorized() throws Exception {
        mvc.perform(get("/reports/pdf")).andExpect(status().isUnauthorized());
    }
}