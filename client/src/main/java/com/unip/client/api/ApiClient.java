package com.unip.client.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unip.client.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
        return get(authorization, b -> b.path("/users/me").build(),
                new ParameterizedTypeReference<UserDto>() {});
    }

    public List<SectorDto> sectors(String authorization) {
        return get(authorization, b -> b.path("/sectors").build(),
                new ParameterizedTypeReference<List<SectorDto>>() {});
    }

    /** Blank or null filters are simply not sent. */
    public List<IncidentDto> searchIncidents(String authorization, String status, String type,
                                             Long sectorId, LocalDate from, LocalDate to) {
        return get(authorization, b -> b.path("/incidents")
                        .queryParamIfPresent("status", present(status))
                        .queryParamIfPresent("type", present(type))
                        .queryParamIfPresent("sectorId", Optional.ofNullable(sectorId))
                        .queryParamIfPresent("from", Optional.ofNullable(from))
                        .queryParamIfPresent("to", Optional.ofNullable(to))
                        .build(),
                new ParameterizedTypeReference<List<IncidentDto>>() {});
    }

    public IncidentDto incident(String authorization, Long id) {
        return get(authorization, b -> b.path("/incidents/{id}").build(id),
                new ParameterizedTypeReference<IncidentDto>() {});
    }

    public List<ActionDto> actions(String authorization, Long incidentId) {
        return get(authorization, b -> b.path("/incidents/{id}/actions").build(incidentId),
                new ParameterizedTypeReference<List<ActionDto>>() {});
    }

    public List<CheckDto> checks(String authorization, Long incidentId) {
        return get(authorization, b -> b.path("/incidents/{id}/checks").build(incidentId),
                new ParameterizedTypeReference<List<CheckDto>>() {});
    }

    public List<HistoryDto> history(String authorization, Long incidentId) {
        return get(authorization, b -> b.path("/incidents/{id}/history").build(incidentId),
                new ParameterizedTypeReference<List<HistoryDto>>() {});
    }

    private <T> T get(String authorization, Function<UriBuilder, URI> uri,
                      ParameterizedTypeReference<T> type) {
        try {
            return rest.get().uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> { throw toException(res); })
                    .body(type);
        } catch (ResourceAccessException e) {
            throw new ApiException(503, "The API is unavailable. Check that it is running.");
        }
    }

    private static Optional<String> present(String value) {
        return (value == null || value.isBlank()) ? Optional.empty() : Optional.of(value.trim());
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