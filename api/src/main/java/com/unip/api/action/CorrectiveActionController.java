package com.unip.api.action;

import com.unip.api.action.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/incidents/{incidentId}/actions")
public class CorrectiveActionController {

    private final CorrectiveActionService service;

    public CorrectiveActionController(CorrectiveActionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ActionResponse> create(@PathVariable Long incidentId,
                                                 @Valid @RequestBody ActionRequest req) {
        CorrectiveAction created = service.create(incidentId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(ActionResponse.from(created));
    }

    @GetMapping
    public List<ActionResponse> list(@PathVariable Long incidentId) {
        return service.list(incidentId).stream().map(ActionResponse::from).toList();
    }
}