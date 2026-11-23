package com.unip.client.web;

import com.unip.client.api.ApiClient;
import com.unip.client.api.ApiException;
import com.unip.client.api.ApiRequests;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class IncidentCommandController {

    private final ApiClient api;

    public IncidentCommandController(ApiClient api) {
        this.api = api;
    }

    @PostMapping("/incidents/{id}/analyze")
    public String analyze(@PathVariable Long id,
                          @SessionAttribute(SessionUser.KEY) SessionUser user,
                          RedirectAttributes redirect) {
        return run(id, redirect, "Analysis started",
                () -> api.analyze(user.getAuthorization(), id));
    }

    @PostMapping("/incidents/{id}/root-cause")
    public String rootCause(@PathVariable Long id, @RequestParam String rootCause,
                            @SessionAttribute(SessionUser.KEY) SessionUser user,
                            RedirectAttributes redirect) {
        return run(id, redirect, "Root cause saved",
                () -> api.saveRootCause(user.getAuthorization(), id, rootCause.trim()));
    }

    @PostMapping("/incidents/{id}/actions")
    public String addAction(@PathVariable Long id, @RequestParam String description,
                            @RequestParam Long responsibleId,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
                            @SessionAttribute(SessionUser.KEY) SessionUser user,
                            RedirectAttributes redirect) {
        return run(id, redirect, "Corrective action added",
                () -> api.addAction(user.getAuthorization(), id,
                        new ApiRequests.Action(description.trim(), responsibleId, deadline)));
    }

    @PostMapping("/incidents/{id}/start-execution")
    public String startExecution(@PathVariable Long id,
                                 @SessionAttribute(SessionUser.KEY) SessionUser user,
                                 RedirectAttributes redirect) {
        return run(id, redirect, "Execution started",
                () -> api.startExecution(user.getAuthorization(), id));
    }

    @PostMapping("/corrective-actions/{actionId}/complete")
    public String complete(@PathVariable Long actionId, @RequestParam Long incidentId,
                           @RequestParam String note,
                           @RequestParam(required = false) String returnTo,
                           @SessionAttribute(SessionUser.KEY) SessionUser user,
                           RedirectAttributes redirect) {
        String toIncident = run(incidentId, redirect, "Action completed",
                () -> api.completeAction(user.getAuthorization(), actionId, note.trim()));
        return "my-actions".equals(returnTo) ? "redirect:/my-actions" : toIncident;
    }

    @PostMapping("/incidents/{id}/checks")
    public String verify(@PathVariable Long id, @RequestParam boolean effective,
                         @RequestParam String opinion,
                         @SessionAttribute(SessionUser.KEY) SessionUser user,
                         RedirectAttributes redirect) {
        return run(id, redirect,
                effective ? "Verified as effective: incident closed"
                        : "Verified as ineffective: a new cycle was started",
                () -> api.verify(user.getAuthorization(), id, effective, opinion.trim()));
    }

    @PostMapping("/incidents/{id}/dismiss")
    public String dismiss(@PathVariable Long id, @RequestParam String justification,
                          @SessionAttribute(SessionUser.KEY) SessionUser user,
                          RedirectAttributes redirect) {
        return run(id, redirect, "Incident dismissed",
                () -> api.dismiss(user.getAuthorization(), id, justification.trim()));
    }

    /** Runs the API call and always goes back to the detail page, with a success or error message. */
    private String run(Long incidentId, RedirectAttributes redirect, String successMessage, Runnable call) {
        try {
            call.run();
            redirect.addFlashAttribute("success", successMessage);
        } catch (ApiException e) {
            if (e.getStatus() == 401) throw e; // session expired: the advice sends the user to login
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/incidents/" + incidentId;
    }
}