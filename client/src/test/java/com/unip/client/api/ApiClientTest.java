package com.unip.client.api;

import com.unip.client.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(ApiClient.class)
class ApiClientTest {

    @Autowired ApiClient api;
    @Autowired MockRestServiceServer server;

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
}