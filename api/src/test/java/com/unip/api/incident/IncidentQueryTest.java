package com.unip.api.incident;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentQueryTest {

    @Autowired MockMvc mvc;

    @Test
    void filterByStatusFollowsTheIncident() throws Exception {
        long id = registerIncident(1, "WASTE");

        mvc.perform(get("/incidents").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]", id).isNotEmpty());

        mvc.perform(post("/incidents/{id}/analyze", id)).andExpect(status().isOk());

        mvc.perform(get("/incidents").param("status", "OPEN"))
                .andExpect(jsonPath("$[?(@.id == %d)]", id).isEmpty());
        mvc.perform(get("/incidents").param("status", "UNDER_ANALYSIS"))
                .andExpect(jsonPath("$[?(@.id == %d)]", id).isNotEmpty());
    }

    @Test
    void filterByTypeAndSector() throws Exception {
        long id = registerIncident(2, "LEAK");

        mvc.perform(get("/incidents").param("type", "LEAK").param("sectorId", "2"))
                .andExpect(jsonPath("$[?(@.id == %d)]", id).isNotEmpty());
        mvc.perform(get("/incidents").param("type", "LEAK").param("sectorId", "3"))
                .andExpect(jsonPath("$[?(@.id == %d)]", id).isEmpty());
        mvc.perform(get("/incidents").param("type", "NOISE").param("sectorId", "2"))
                .andExpect(jsonPath("$[?(@.id == %d)]", id).isEmpty());
    }

    @Test
    void filterByPeriod() throws Exception {
        long id = registerIncident(1, "WASTE"); // occurredOn = 2026-10-28

        mvc.perform(get("/incidents").param("from", "2026-10-28").param("to", "2026-10-28"))
                .andExpect(jsonPath("$[?(@.id == %d)]", id).isNotEmpty());
        mvc.perform(get("/incidents").param("from", "2026-10-29"))
                .andExpect(jsonPath("$[?(@.id == %d)]", id).isEmpty());
    }

    @Test
    void fromAfterToIsBadRequest() throws Exception {
        mvc.perform(get("/incidents").param("from", "2026-11-10").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidStatusIsBadRequest() throws Exception {
        mvc.perform(get("/incidents").param("status", "FOO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historyListsTransitionsInOrder() throws Exception {
        long id = registerIncident(1, "WASTE");
        mvc.perform(post("/incidents/{id}/analyze", id)).andExpect(status().isOk());

        mvc.perform(get("/incidents/{id}/history", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].previousStatus").value("OPEN"))
                .andExpect(jsonPath("$[0].newStatus").value("UNDER_ANALYSIS"));
    }

    @Test
    void historyOfUnknownIncidentIsNotFound() throws Exception {
        mvc.perform(get("/incidents/{id}/history", 999999)).andExpect(status().isNotFound());
        mvc.perform(get("/incidents/{id}/checks", 999999)).andExpect(status().isNotFound());
    }

    private long registerIncident(int sectorId, String type) throws Exception {
        String json = """
            {"title":"Test incident","description":"Test",
             "type":"%s","severity":"LOW","sectorId":%d,
             "occurredOn":"2026-10-28"}""".formatted(type, sectorId);

        String body = mvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}