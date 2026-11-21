package com.unip.client.api;

import com.unip.client.dto.IncidentDto;
import com.unip.client.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(ApiClient.class)
class ApiClientTest {

    @Autowired ApiClient api;
    @Autowired MockRestServiceServer server;

    private static final String INCIDENT_JSON = """
    {"id":1,"title":"Dripping tap","description":"Drips","type":"WASTE","severity":"LOW",
     "status":"OPEN","sector":"Maintenance","occurredOn":"2026-10-28",
     "registeredAt":"2026-10-28T10:15:30.123456","cycle":1,"rootCause":null,
     "closedAt":null,"registeredBy":"Employee One"}""";

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
}