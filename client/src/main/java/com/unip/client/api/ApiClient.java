package com.unip.client.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unip.client.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
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

    // ---------- reads ----------

    public UserDto me(String auth) {
        return get(auth, b -> b.path("/users/me").build(), new ParameterizedTypeReference<UserDto>() {});
    }

    public List<UserDto> users(String auth) {
        return get(auth, b -> b.path("/users").build(), new ParameterizedTypeReference<List<UserDto>>() {});
    }

    public List<SectorDto> sectors(String auth) {
        return get(auth, b -> b.path("/sectors").build(), new ParameterizedTypeReference<List<SectorDto>>() {});
    }

    public SummaryDto summary(String auth) {
        return get(auth, b -> b.path("/reports/summary").build(),
                new ParameterizedTypeReference<SummaryDto>() {});
    }

    public List<OverdueDto> overdue(String auth) {
        return get(auth, b -> b.path("/reports/overdue").build(),
                new ParameterizedTypeReference<List<OverdueDto>>() {});
    }

    public List<MyActionDto> myActions(String auth) {
        return get(auth, b -> b.path("/corrective-actions/mine").build(),
                new ParameterizedTypeReference<List<MyActionDto>>() {});
    }

    /** Blank or null filters are simply not sent. */
    public List<IncidentDto> searchIncidents(String auth, String status, String type,
                                             Long sectorId, LocalDate from, LocalDate to) {
        return get(auth, b -> b.path("/incidents")
                        .queryParamIfPresent("status", present(status))
                        .queryParamIfPresent("type", present(type))
                        .queryParamIfPresent("sectorId", Optional.ofNullable(sectorId))
                        .queryParamIfPresent("from", Optional.ofNullable(from))
                        .queryParamIfPresent("to", Optional.ofNullable(to))
                        .build(),
                new ParameterizedTypeReference<List<IncidentDto>>() {});
    }

    public IncidentDto incident(String auth, Long id) {
        return get(auth, b -> b.path("/incidents/{id}").build(id), new ParameterizedTypeReference<IncidentDto>() {});
    }

    public List<ActionDto> actions(String auth, Long incidentId) {
        return get(auth, b -> b.path("/incidents/{id}/actions").build(incidentId),
                new ParameterizedTypeReference<List<ActionDto>>() {});
    }

    public List<CheckDto> checks(String auth, Long incidentId) {
        return get(auth, b -> b.path("/incidents/{id}/checks").build(incidentId),
                new ParameterizedTypeReference<List<CheckDto>>() {});
    }

    public List<HistoryDto> history(String auth, Long incidentId) {
        return get(auth, b -> b.path("/incidents/{id}/history").build(incidentId),
                new ParameterizedTypeReference<List<HistoryDto>>() {});
    }

    // ---------- commands ----------

    public IncidentDto registerIncident(String auth, ApiRequests.Incident body) {
        return exchange(HttpMethod.POST, auth, b -> b.path("/incidents").build(), body,
                new ParameterizedTypeReference<IncidentDto>() {});
    }

    public IncidentDto analyze(String auth, Long id) {
        return exchange(HttpMethod.POST, auth, b -> b.path("/incidents/{id}/analyze").build(id), null,
                new ParameterizedTypeReference<IncidentDto>() {});
    }

    public IncidentDto saveRootCause(String auth, Long id, String rootCause) {
        return exchange(HttpMethod.PUT, auth, b -> b.path("/incidents/{id}/root-cause").build(id),
                new ApiRequests.RootCause(rootCause), new ParameterizedTypeReference<IncidentDto>() {});
    }

    public ActionDto addAction(String auth, Long incidentId, ApiRequests.Action body) {
        return exchange(HttpMethod.POST, auth, b -> b.path("/incidents/{id}/actions").build(incidentId), body,
                new ParameterizedTypeReference<ActionDto>() {});
    }

    public IncidentDto startExecution(String auth, Long id) {
        return exchange(HttpMethod.POST, auth, b -> b.path("/incidents/{id}/start-execution").build(id), null,
                new ParameterizedTypeReference<IncidentDto>() {});
    }

    public ActionDto completeAction(String auth, Long actionId, String note) {
        return exchange(HttpMethod.POST, auth, b -> b.path("/corrective-actions/{id}/complete").build(actionId),
                new ApiRequests.CompleteAction(note), new ParameterizedTypeReference<ActionDto>() {});
    }

    public CheckDto verify(String auth, Long incidentId, boolean effective, String opinion) {
        return exchange(HttpMethod.POST, auth, b -> b.path("/incidents/{id}/checks").build(incidentId),
                new ApiRequests.Check(effective, opinion), new ParameterizedTypeReference<CheckDto>() {});
    }

    public IncidentDto dismiss(String auth, Long id, String justification) {
        return exchange(HttpMethod.POST, auth, b -> b.path("/incidents/{id}/dismiss").build(id),
                new ApiRequests.Dismiss(justification), new ParameterizedTypeReference<IncidentDto>() {});
    }

    // ---------- plumbing ----------

    private <T> T get(String auth, Function<UriBuilder, URI> uri, ParameterizedTypeReference<T> type) {
        return exchange(HttpMethod.GET, auth, uri, null, type);
    }

    private <T> T exchange(HttpMethod method, String auth, Function<UriBuilder, URI> uri,
                           Object body, ParameterizedTypeReference<T> type) {
        try {
            RestClient.RequestBodySpec spec = rest.method(method).uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, auth);
            if (body != null) {
                spec = spec.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            return spec.retrieve()
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
        String message = null;
        try {
            JsonNode root = mapper.readTree(res.getBody());
            message = root.path("detail").asText(null);
            JsonNode errors = root.path("errors"); // field errors of a 400
            if (message != null && errors.isObject() && errors.size() > 0) {
                List<String> parts = new ArrayList<>();
                errors.fields().forEachRemaining(e -> parts.add(e.getKey() + ": " + e.getValue().asText()));
                message += " (" + String.join("; ", parts) + ")";
            }
        } catch (Exception ignored) {
            // body was not a ProblemDetail
        }
        return new ApiException(status, message != null ? message : fallbackMessage(status));
    }

    private static String fallbackMessage(int status) {
        return switch (status) {
            case 403 -> "You do not have permission for this operation";
            case 404 -> "Not found";
            default -> "Unexpected error (" + status + ")";
        };
    }
}