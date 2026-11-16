package com.unip.api.action;

import com.unip.api.action.dto.ActionResponse;
import com.unip.api.action.dto.CompleteActionRequest;
import com.unip.api.incident.IncidentWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/corrective-actions")
public class ActionCompletionController {

    private final IncidentWorkflowService workflow;

    public ActionCompletionController(IncidentWorkflowService workflow) {
        this.workflow = workflow;
    }

    @PostMapping("/{id}/complete")
    public ActionResponse complete(@PathVariable Long id,
                                   @Valid @RequestBody CompleteActionRequest req) {
        return ActionResponse.from(workflow.completeAction(id, req.note()));
    }
}