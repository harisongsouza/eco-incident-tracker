package com.unip.api.incident;

import com.unip.api.check.EffectivenessCheck;
import com.unip.api.check.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import com.unip.api.incident.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

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

    @PostMapping("/{id}/checks")
    public ResponseEntity<CheckResponse> verify(@PathVariable Long id,
                                                @Valid @RequestBody CheckRequest req) {
        EffectivenessCheck check = workflow.verify(id, req.effective(), req.opinion());
        return ResponseEntity.status(HttpStatus.CREATED).body(CheckResponse.from(check));
    }

    @PostMapping("/{id}/dismiss")
    public IncidentResponse dismiss(@PathVariable Long id,
                                    @Valid @RequestBody DismissRequest req) {
        return IncidentResponse.from(workflow.dismiss(id, req.justification()));
    }

    @GetMapping("/{id}")
    public IncidentResponse findById(@PathVariable Long id) {
        return IncidentResponse.from(service.findById(id));
    }

    @GetMapping
    public List<IncidentResponse> search(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) Long sectorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.search(status, type, sectorId, from, to).stream()
                .map(IncidentResponse::from).toList();
    }

    @PutMapping("/{id}/root-cause")
    public IncidentResponse registerRootCause(@PathVariable Long id,
                                              @Valid @RequestBody RootCauseRequest req) {
        return IncidentResponse.from(service.registerRootCause(id, req.rootCause()));
    }

    @PostMapping("/{id}/start-execution")
    public IncidentResponse startExecution(@PathVariable Long id) {
        return IncidentResponse.from(workflow.startExecution(id));
    }
}