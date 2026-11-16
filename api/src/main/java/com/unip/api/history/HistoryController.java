package com.unip.api.history;

import com.unip.api.history.dto.HistoryResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/incidents/{incidentId}/history")
public class HistoryController {

    private final HistoryService service;

    public HistoryController(HistoryService service) { this.service = service; }

    @GetMapping
    public List<HistoryResponse> list(@PathVariable Long incidentId) {
        return service.list(incidentId).stream().map(HistoryResponse::from).toList();
    }
}