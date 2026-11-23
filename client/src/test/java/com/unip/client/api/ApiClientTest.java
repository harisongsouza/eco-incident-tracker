package com.unip.client.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unip.client.dto.IncidentDto;
import com.unip.client.dto.MyActionDto;
import com.unip.client.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.unip.client.dto.SummaryDto;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ApiClientTest {

    private static final String INCIDENT_JSON = """
        {"id":1,"title":"Dripping tap","description":"Drips","type":"WASTE","severity":"LOW",
         "status":"OPEN","sector":"Maintenance","occurredOn":"2026-10-28",
         "registeredAt":"2026-10-28T10:15:30.123456","cycle":1,"rootCause":null,
         "closedAt":null,"registeredBy":"Employee One"}""";

    private MockRestServiceServer server;
    private ApiClient api;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build(); // bind BEFORE building the client
        api = new ApiClient(builder, "http://localhost:8080", new ObjectMapper());
    }

    @Test
    void meSendsAuthorizationAndParsesUser() {
        server.expect(requestTo("http://localhost:8080/users/me"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic abc"))
                .andRespond(withSuccess("""
                {"id":3,"name":"Environmental Officer","email":"officer@demo.com",
                 "role":"ENVIRONMENTAL_OFFICER","active":true}""", MediaType.APPLICATION_JSON));

        UserDto user = api.me("Basic abc");

        assertEquals("Environmental Officer", user.name());
        assertEquals("ENVIRONMENTAL_OFFICER", user.role());
    }

    @Test
    void unauthorizedBecomesApiExceptionWithStatus401() {
        server.expect(requestTo("http://localhost:8080/users/me"))
                .andRespond(withUnauthorizedRequest());

        ApiException ex = assertThrows(ApiException.class, () -> api.me("Basic wrong"));

        assertEquals(401, ex.getStatus());
    }

    @Test
    void problemDetailMessageIsPropagated() {
        server.expect(requestTo("http://localhost:8080/users/me"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"Cannot move incident from OPEN to CLOSED\"}"));

        ApiException ex = assertThrows(ApiException.class, () -> api.me("Basic abc"));

        assertEquals(409, ex.getStatus());
        assertEquals("Cannot move incident from OPEN to CLOSED", ex.getMessage());
    }

    @Test
    void basicAuthEncodesCredentials() {
        assertEquals("Basic YUBiLmNvbTpwdw==", ApiClient.basicAuth("a@b.com", "pw"));
    }

    @Test
    void searchSendsOnlyTheFilledFilters() {
        server.expect(requestTo("http://localhost:8080/incidents?status=OPEN&sectorId=1&from=2026-10-01"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic abc"))
                .andRespond(withSuccess("[" + INCIDENT_JSON + "]", MediaType.APPLICATION_JSON));

        List<IncidentDto> result = api.searchIncidents(
                "Basic abc", "OPEN", "", 1L, LocalDate.of(2026, 10, 1), null);

        assertEquals(1, result.size());
        assertEquals("Dripping tap", result.get(0).title());
        assertEquals(LocalDate.of(2026, 10, 28), result.get(0).occurredOn());
    }

    @Test
    void searchWithoutFiltersHasNoQueryString() {
        server.expect(requestTo("http://localhost:8080/incidents"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertEquals(0, api.searchIncidents("Basic abc", null, null, null, null, null).size());
    }

    @Test
    void notFoundFromApiKeepsItsMessage() {
        server.expect(requestTo("http://localhost:8080/incidents/99"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"Incident not found\"}"));

        ApiException ex = assertThrows(ApiException.class, () -> api.incident("Basic abc", 99L));

        assertEquals(404, ex.getStatus());
        assertEquals("Incident not found", ex.getMessage());
    }

    @Test
    void analyzeIsAPostWithoutBody() {
        server.expect(requestTo("http://localhost:8080/incidents/1/analyze"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic abc"))
                .andRespond(withSuccess(INCIDENT_JSON.replace("\"OPEN\"", "\"UNDER_ANALYSIS\""),
                        MediaType.APPLICATION_JSON));

        assertEquals("UNDER_ANALYSIS", api.analyze("Basic abc", 1L).status());
    }

    @Test
    void registerIncidentSendsTheJsonBody() {
        server.expect(requestTo("http://localhost:8080/incidents"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Dripping tap"))
                .andExpect(jsonPath("$.sectorId").value(1))
                .andExpect(jsonPath("$.occurredOn").value("2026-10-28"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON).body(INCIDENT_JSON));

        IncidentDto created = api.registerIncident("Basic abc", new ApiRequests.Incident(
                "Dripping tap", "Drips", "WASTE", "LOW", 1L, LocalDate.of(2026, 10, 28)));

        assertEquals(1L, created.id());
    }

    @Test
    void fieldErrorsAreAppendedToTheMessage() {
        server.expect(requestTo("http://localhost:8080/incidents"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"Validation failed\",\"errors\":{\"title\":\"must not be blank\"}}"));

        ApiException ex = assertThrows(ApiException.class, () -> api.registerIncident("Basic abc",
                new ApiRequests.Incident("", "x", "WASTE", "LOW", 1L, LocalDate.of(2026, 10, 28))));

        assertEquals(400, ex.getStatus());
        assertEquals("Validation failed (title: must not be blank)", ex.getMessage());
    }

    @Test
    void summaryParsesTheMapsKeepingTheirOrder() {
        server.expect(requestTo("http://localhost:8080/reports/summary"))
                .andRespond(withSuccess("""
            {"total":3,"byStatus":{"OPEN":2,"CLOSED":1},
             "byType":{"LEAK":3},"bySector":{"Maintenance":3}}""", MediaType.APPLICATION_JSON));

        SummaryDto summary = api.summary("Basic abc");

        assertEquals(3, summary.total());
        assertEquals(2L, summary.byStatus().get("OPEN"));
        assertEquals(List.of("OPEN", "CLOSED"), List.copyOf(summary.byStatus().keySet()));
    }

    @Test
    void myActionsParsesTheList() {
        server.expect(requestTo("http://localhost:8080/corrective-actions/mine"))
                .andRespond(withSuccess("""
            [{"actionId":7,"incidentId":1,"incidentTitle":"Dripping tap",
              "incidentStatus":"IN_PROGRESS","description":"Replace gasket",
              "deadline":"2026-11-30","cycle":1}]""", MediaType.APPLICATION_JSON));

        List<MyActionDto> result = api.myActions("Basic abc");

        assertEquals(1, result.size());
        assertEquals("IN_PROGRESS", result.get(0).incidentStatus());
        assertEquals(LocalDate.of(2026, 11, 30), result.get(0).deadline());
    }

    @Test
    void forbiddenWithoutBodyGetsAReadableMessage() {
        server.expect(requestTo("http://localhost:8080/reports/summary"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        ApiException ex = assertThrows(ApiException.class, () -> api.summary("Basic abc"));

        assertEquals(403, ex.getStatus());
        assertEquals("You do not have permission for this operation", ex.getMessage());
    }
}