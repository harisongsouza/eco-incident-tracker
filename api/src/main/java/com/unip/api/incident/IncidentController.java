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

    public IncidentController(IncidentService service) {
        this.service = service;
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