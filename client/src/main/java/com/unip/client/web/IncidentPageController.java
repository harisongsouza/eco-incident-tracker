package com.unip.client.web;

import com.unip.client.api.ApiClient;
import com.unip.client.api.ApiException;
import com.unip.client.api.ApiRequests;
import com.unip.client.dto.ActionDto;
import com.unip.client.dto.IncidentDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private static final List<String> SEVERITIES = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

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

    @GetMapping("/new")
    public String newForm(@SessionAttribute(SessionUser.KEY) SessionUser user, Model model) {
        model.addAttribute("form", new IncidentForm());
        fillFormOptions(model, user.getAuthorization());
        return "incident-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") IncidentForm form, BindingResult result,
                         @SessionAttribute(SessionUser.KEY) SessionUser user,
                         Model model, RedirectAttributes redirect) {
        String auth = user.getAuthorization();
        if (!result.hasErrors()) {
            try {
                IncidentDto created = api.registerIncident(auth, new ApiRequests.Incident(
                        form.getTitle().trim(), form.getDescription().trim(), form.getType(),
                        form.getSeverity(), form.getSectorId(), form.getOccurredOn()));
                redirect.addFlashAttribute("success", "Incident registered");
                return "redirect:/incidents/" + created.id();
            } catch (ApiException e) {
                if (e.getStatus() == 401) throw e;
                model.addAttribute("error", e.getMessage()); // the API is the final authority
            }
        }
        fillFormOptions(model, auth);
        return "incident-form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @SessionAttribute(SessionUser.KEY) SessionUser user,
                         Model model) {
        String auth = user.getAuthorization();
        IncidentDto incident = api.incident(auth, id); // 404 if not visible to this user
        List<ActionDto> actions = api.actions(auth, id);

        model.addAttribute("incident", incident);
        model.addAttribute("actions", actions);
        model.addAttribute("checks", api.checks(auth, id));
        model.addAttribute("history", api.history(auth, id));
        model.addAttribute("hasActionsInCycle",
                actions.stream().anyMatch(a -> a.cycle() == incident.cycle()));

        // only the officer assigns actions, and /users is restricted to officer and manager
        if (user.isOfficer() && "UNDER_ANALYSIS".equals(incident.status())) {
            model.addAttribute("users", api.users(auth));
        }
        return "incident-detail";
    }

    private void fillFormOptions(Model model, String auth) {
        model.addAttribute("sectors", api.sectors(auth));
        model.addAttribute("types", TYPES);
        model.addAttribute("severities", SEVERITIES);
    }
}