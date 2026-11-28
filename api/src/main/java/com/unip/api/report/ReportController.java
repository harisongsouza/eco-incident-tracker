package com.unip.api.report;

import com.unip.api.report.dto.OverdueActionResponse;
import com.unip.api.report.dto.SummaryResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('ENVIRONMENTAL_OFFICER','MANAGER')")
public class ReportController {

    private final ReportService service;
    private final ReportPdfGenerator pdfGenerator;

    public ReportController(ReportService service, ReportPdfGenerator pdfGenerator) {
        this.service = service;
        this.pdfGenerator = pdfGenerator;
    }

    @GetMapping("/summary")
    public SummaryResponse summary() { return service.summary(); }

    @GetMapping("/overdue")
    public List<OverdueActionResponse> overdue() { return service.overdue(); }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf() {
        byte[] pdf = pdfGenerator.build(service.summary(), service.overdue(), LocalDateTime.now());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("incident-report.pdf").build().toString())
                .body(pdf);
    }
}