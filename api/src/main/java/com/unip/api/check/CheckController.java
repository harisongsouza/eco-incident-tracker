package com.unip.api.check;

import com.unip.api.check.dto.CheckResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/incidents/{incidentId}/checks")
public class CheckController {

    private final CheckService service;

    public CheckController(CheckService service) { this.service = service; }

    @GetMapping
    public List<CheckResponse> list(@PathVariable Long incidentId) {
        return service.list(incidentId).stream().map(CheckResponse::from).toList();
    }
}