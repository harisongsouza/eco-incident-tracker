package com.unip.client.web;

import com.unip.client.api.ApiClient;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class ReportPageController {

    private final ApiClient api;

    public ReportPageController(ApiClient api) {
        this.api = api;
    }

    @GetMapping("/reports")
    public String reports(@SessionAttribute(SessionUser.KEY) SessionUser user, Model model) {
        String auth = user.getAuthorization();
        model.addAttribute("summary", api.summary(auth)); // 403 for employees: the API decides
        model.addAttribute("overdue", api.overdue(auth));
        return "reports";
    }

    @GetMapping("/reports/pdf")
    public ResponseEntity<byte[]> pdf(@SessionAttribute(SessionUser.KEY) SessionUser user) {
        byte[] pdf = api.reportPdf(user.getAuthorization()); // the client only relays the file
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("incident-report.pdf").build().toString())
                .body(pdf);
    }
}