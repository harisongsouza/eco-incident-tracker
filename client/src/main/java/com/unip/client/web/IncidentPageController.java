package com.unip.client.web;

import com.unip.client.api.ApiClient;
import com.unip.client.api.ApiException;
import com.unip.client.dto.IncidentDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/incidents")
public class IncidentPageController {

    // same values as the API enums; the client depends only on the HTTP contract
    private static final List<String> STATUSES = List.of(
            "OPEN", "UNDER_ANALYSIS", "IN_PROGRESS", "AWAITING_VERIFICATION", "CLOSED", "DISMISSED");
    private static final List<String> TYPES = List.of(
            "LEAK", "IMPROPER_DISPOSAL", "WASTE", "EMISSION", "NOISE", "OTHER");

    private final ApiClient api;

    public IncidentPageController(ApiClient api) {
        this.api = api;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) Long sectorId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @SessionAttribute(SessionUser.KEY) SessionUser user,
                       Model model) {
        String auth = user.getAuthorization();

        List<IncidentDto> incidents;
        try {
            incidents = api.searchIncidents(auth, status, type, sectorId, from, to);
        } catch (ApiException e) {
            if (e.getStatus() != 400) throw e; // 400 = invalid filter (e.g. period): show inline
            model.addAttribute("error", e.getMessage());
            incidents = List.of();
        }

        model.addAttribute("incidents", incidents);
        model.addAttribute("sectors", api.sectors(auth));
        model.addAttribute("statuses", STATUSES);
        model.addAttribute("types", TYPES);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("sectorId", sectorId);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "incidents";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @SessionAttribute(SessionUser.KEY) SessionUser user,
                         Model model) {
        String auth = user.getAuthorization();
        model.addAttribute("incident", api.incident(auth, id)); // 404 if not visible to this user
        model.addAttribute("actions", api.actions(auth, id));
        model.addAttribute("checks", api.checks(auth, id));
        model.addAttribute("history", api.history(auth, id));
        return "incident-detail";
    }
}