package com.unip.api.report;

import com.unip.api.report.dto.OverdueActionResponse;
import com.unip.api.report.dto.SummaryResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) { this.service = service; }

    @GetMapping("/summary")
    public SummaryResponse summary() { return service.summary(); }

    @GetMapping("/overdue")
    public List<OverdueActionResponse> overdue() { return service.overdue(); }
}