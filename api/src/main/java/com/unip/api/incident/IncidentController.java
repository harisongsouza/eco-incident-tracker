package com.unip.api.incident;

import com.unip.api.incident.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;

@RestController
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService service;
    private final IncidentWorkflowService workflow;

    public IncidentController(IncidentService service, IncidentWorkflowService workflow) {
        this.service = service;
        this.workflow = workflow;
    }

    @PostMapping("/{id}/analyze")
    public IncidentResponse analyze(@PathVariable Long id) {
        return IncidentResponse.from(workflow.startAnalysis(id));
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> register(@Valid @RequestBody IncidentRequest req) {
        Incident created = service.register(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(IncidentResponse.from(created));
    }

    @GetMapping("/{id}")
    public IncidentResponse findById(@PathVariable Long id) {
        return IncidentResponse.from(service.findById(id));
    }
}