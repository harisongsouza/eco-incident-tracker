package com.unip.client.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unip.client.dto.UserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class ApiClient {

    private final RestClient rest;
    private final ObjectMapper mapper;

    public ApiClient(RestClient.Builder builder,
                     @Value("${api.base-url}") String baseUrl,
                     ObjectMapper mapper) {
        this.rest = builder.baseUrl(baseUrl).build();
        this.mapper = mapper;
    }

    public static String basicAuth(String email, String password) {
        String raw = email + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public UserDto me(String authorization) {
        return get("/users/me", authorization, UserDto.class);
    }

    private <T> T get(String path, String authorization, Class<T> type) {
        try {
            return rest.get().uri(path)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                    .body(type);
        } catch (ResourceAccessException e) {
            throw new ApiException(503, "The API is unavailable. Check that it is running.");
        }
    }

    private ApiException toException(ClientHttpResponse res) throws IOException {
        int status = res.getStatusCode().value();
        String detail = null;
        try {
            detail = mapper.readTree(res.getBody()).path("detail").asText(null);
        } catch (Exception ignored) {
            // body was not a ProblemDetail
        }
        return new ApiException(status, detail != null ? detail : "Unexpected error (" + status + ")");
    }
}